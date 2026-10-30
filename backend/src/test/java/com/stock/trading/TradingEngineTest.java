package com.stock.trading;

import com.stock.trading.adapter.*;
import com.stock.trading.dto.*;
import com.stock.trading.enums.*;
import com.stock.trading.exception.TradingException;
import com.stock.trading.model.*;
import com.stock.trading.port.*;
import com.stock.trading.repository.*;
import com.stock.trading.service.*;
import com.stock.trading.store.TradingUnitOfWork;
import com.stock.trading.store.TradingUnitOfWork.HoldingKey;
import jakarta.validation.*;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TradingEngineTest {
    static final UUID USER=UUID.fromString("11111111-1111-4111-8111-111111111111");
    static final ValidatorFactory VALIDATORS=Validation.buildDefaultValidatorFactory();
    TradingUnitOfWork work;
    InMemoryAccountGateway accounts;
    InMemoryMarketGateway market;
    OrderRepository orderRepository;
    TradeRepository tradeRepository;
    TradingService trading;
    OrderService orders;
    MutableClock clock;

    @BeforeEach void setup() {
        work=new TradingUnitOfWork(); accounts=spy(new InMemoryAccountGateway(work)); market=new InMemoryMarketGateway(work,false);
        orderRepository=new OrderRepository(work); tradeRepository=spy(new TradeRepository(work));
        clock=new MutableClock("2026-10-30T04:30:00Z");
        var brokerage=new BrokerageService();
        var execution=new OrderExecutionService(accounts,brokerage,orderRepository,tradeRepository);
        trading=new TradingService(work,accounts,market,orderRepository,brokerage,execution,clock,VALIDATORS.getValidator());
        orders=new OrderService(work,orderRepository,tradeRepository,accounts,execution,clock);
        work.execute(()->{
            work.state().accounts.put(USER,new AccountSnapshot(USER,"Test Trader",true,false,bd("100000"),Money.ZERO));
            work.state().stocks.put("TCS",new StockQuote("TCS","Tata Consultancy Services","IT",bd("100"),bd("100"),true));
            work.state().holdings.put(new HoldingKey(USER,"TCS"),new HoldingSnapshot(USER,"TCS",10,0,bd("80")));
            return null;
        });
    }
    @AfterAll static void closeValidator() { VALIDATORS.close(); }
    static BigDecimal bd(String v) { return new BigDecimal(v).setScale(2,RoundingMode.HALF_UP); }
    PlaceOrderRequest request(OrderSide side,OrderType type,int qty,String limit) { return new PlaceOrderRequest("tcs",side,type,qty,limit==null?null:bd(limit)); }
    OrderResponse place(OrderSide side,OrderType type,int qty,String limit) { return trading.place(USER,request(side,type,qty,limit),null); }
    AccountSnapshot account() { return work.execute(()->accounts.account(USER)); }
    HoldingSnapshot holding() { return work.execute(()->accounts.holding(USER,"TCS")); }
    List<Trade> trades() { return orders.trades(USER,USER); }
    void price(String price) { work.execute(()->{ var q=market.quote("TCS"); work.state().stocks.put("TCS",new StockQuote(q.symbol(),q.companyName(),q.sector(),bd(price),q.previousClose(),q.active())); return null; }); }
    void flags(boolean kyc,boolean blocked) { work.execute(()->{ var a=accounts.account(USER); work.state().accounts.put(USER,new AccountSnapshot(USER,a.name(),kyc,blocked,a.availableBalance(),a.blockedBalance())); return null; }); }
    void balance(String value) { work.execute(()->{ var a=accounts.account(USER); work.state().accounts.put(USER,new AccountSnapshot(USER,a.name(),a.kycApproved(),a.blocked(),bd(value),a.blockedBalance())); return null; }); }
    void suspended() { work.execute(()->{ var q=market.quote("TCS"); work.state().stocks.put("TCS",new StockQuote(q.symbol(),q.companyName(),q.sector(),q.price(),q.previousClose(),false)); return null; }); }

    @Test void marketBuySettlesAllRecordsAndWeightedAverage() {
        var order=place(OrderSide.BUY,OrderType.MARKET,10,null);
        assertThat(order.status()).isEqualTo(OrderStatus.EXECUTED);
        assertThat(account().availableBalance()).isEqualByComparingTo("98999.41");
        assertThat(account().blockedBalance()).isZero();
        assertThat(holding().quantity()).isEqualTo(20);
        assertThat(holding().averageBuyPrice()).isEqualByComparingTo("90");
        assertThat(trades()).hasSize(1);
        var trade=trades().getFirst();
        assertThat(trade.orderId()).isEqualTo(order.id()); assertThat(trade.id()).isEqualTo(order.tradeId());
        assertThat(trade.netCashFlow()).isEqualByComparingTo("-1000.59");
        assertThat(order.reservedCash()).isZero();
    }
    @Test void marketSellCreditsNetProceedsAndRealizedPnlAndRemovesZeroHolding() {
        place(OrderSide.SELL,OrderType.MARKET,10,null);
        assertThat(account().availableBalance()).isEqualByComparingTo("100999.41");
        assertThat(holding().quantity()).isZero();
        assertThat(work.execute(()->accounts.holdings(USER))).isEmpty();
        assertThat(trades().getFirst().realizedPnl()).isEqualByComparingTo("200");
        assertThat(trades().getFirst().gst()).isEqualByComparingTo("0.09");
    }
    @Test void partialSellPreservesAverageAndCanRealizeLoss() {
        price("70"); place(OrderSide.SELL,OrderType.MARKET,4,null);
        assertThat(holding().quantity()).isEqualTo(6); assertThat(holding().averageBuyPrice()).isEqualByComparingTo("80");
        assertThat(trades().getFirst().realizedPnl()).isEqualByComparingTo("-40");
    }
    @Test void limitBuyReservesFullBudgetIncludingFeesThenReleasesPriceImprovement() {
        var o=place(OrderSide.BUY,OrderType.LIMIT,10,"90");
        assertThat(o.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(account().blockedBalance()).isEqualByComparingTo("900.53");
        assertThat(account().availableBalance()).isEqualByComparingTo("99099.47");
        price("85"); trading.processOrder(o.id());
        assertThat(orders.get(USER,o.id()).executionPrice()).isEqualByComparingTo("85");
        assertThat(account().blockedBalance()).isZero();
        assertThat(account().availableBalance()).isEqualByComparingTo("99149.49");
        assertThat(holding().averageBuyPrice()).isEqualByComparingTo("82.5");
        assertThat(trades().getFirst().totalCharges()).isEqualByComparingTo("0.51");
    }
    @ParameterizedTest @CsvSource({"BUY,100,EXECUTED","BUY,105,EXECUTED","BUY,99,PENDING","SELL,100,EXECUTED","SELL,95,EXECUTED","SELL,101,PENDING"})
    void limitsCrossAtInclusiveBoundary(OrderSide side,String limit,OrderStatus status) {
        var order=place(side,OrderType.LIMIT,1,limit); assertThat(order.status()).isEqualTo(status);
        if(status==OrderStatus.EXECUTED) assertThat(order.executionPrice()).isEqualByComparingTo("100");
    }
    @Test void pendingLimitDoesNotTradeUntilCrossed() {
        var o=place(OrderSide.BUY,OrderType.LIMIT,1,"90"); trading.processOrder(o.id());
        assertThat(orders.get(USER,o.id()).status()).isEqualTo(OrderStatus.PENDING); assertThat(trades()).isEmpty();
    }
    @Test void pendingSellReservesSharesAndExecutesAtBetterPrice() {
        var o=place(OrderSide.SELL,OrderType.LIMIT,6,"110"); assertThat(holding().reservedQuantity()).isEqualTo(6);
        price("120"); trading.processOrder(o.id());
        assertThat(holding().quantity()).isEqualTo(4); assertThat(holding().reservedQuantity()).isZero();
        assertThat(trades().getFirst().price()).isEqualByComparingTo("120");
    }
    @Test void cannotSellReservedOrUnownedShares() {
        place(OrderSide.SELL,OrderType.LIMIT,8,"110");
        var rejected=place(OrderSide.SELL,OrderType.MARKET,3,null);
        assertThat(rejected.status()).isEqualTo(OrderStatus.REJECTED); assertThat(rejected.reason()).contains("INSUFFICIENT_SHARES");
        assertThat(holding().quantity()).isEqualTo(10); assertThat(holding().reservedQuantity()).isEqualTo(8);
    }
    @Test void fundsMustIncludeChargesAndBlockedCashCannotBeSpent() {
        balance("100"); assertThat(place(OrderSide.BUY,OrderType.MARKET,1,null).status()).isEqualTo(OrderStatus.REJECTED);
        assertThat(account().availableBalance()).isEqualByComparingTo("100");
        balance("200"); place(OrderSide.BUY,OrderType.LIMIT,2,"90");
        assertThat(place(OrderSide.BUY,OrderType.MARKET,1,null).reason()).contains("INSUFFICIENT_FUNDS");
    }
    @Test void exactFundsCanBeSpentWithoutGoingNegative() {
        balance("100.06"); assertThat(place(OrderSide.BUY,OrderType.MARKET,1,null).status()).isEqualTo(OrderStatus.EXECUTED);
        assertThat(account().availableBalance()).isZero();
    }
    @ParameterizedTest @EnumSource(OrderSide.class)
    void cancellationReleasesOnlyItsReservation(OrderSide side) {
        var first=place(side,OrderType.LIMIT,2,side==OrderSide.BUY?"90":"110");
        var second=place(side,OrderType.LIMIT,3,side==OrderSide.BUY?"90":"110");
        assertThat(orders.cancel(USER,first.id()).status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(account().blockedBalance()).isEqualByComparingTo(second.reservedCash());
        assertThat(holding().reservedQuantity()).isEqualTo(second.reservedShares());
        assertThatThrownBy(()->orders.cancel(USER,first.id())).isInstanceOf(TradingException.class);
    }
    @Test void executedOrderCannotBeCancelled() { var o=place(OrderSide.BUY,OrderType.MARKET,1,null); assertThatThrownBy(()->orders.cancel(USER,o.id())).isInstanceOf(TradingException.class); }
    @ParameterizedTest @EnumSource(OrderSide.class)
    void expiresExactlyAtCloseBeforeConsideringCrossingPrice(OrderSide side) {
        var o=place(side,OrderType.LIMIT,2,side==OrderSide.BUY?"90":"110");
        price(side==OrderSide.BUY?"80":"120"); clock.set("2026-10-30T10:00:00Z"); trading.processOrder(o.id());
        assertThat(orders.get(USER,o.id()).status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(orders.get(USER,o.id()).reason()).contains("EXPIRED");
        assertThat(account().blockedBalance()).isZero(); assertThat(holding().reservedQuantity()).isZero(); assertThat(trades()).isEmpty();
    }
    @Test void missedSchedulerSessionExpiresOnNextDayWithoutExecuting() {
        var o=place(OrderSide.BUY,OrderType.LIMIT,1,"90"); price("80"); clock.set("2026-11-02T04:30:00Z"); trading.processOrder(o.id());
        assertThat(orders.get(USER,o.id()).status()).isEqualTo(OrderStatus.CANCELLED); assertThat(trades()).isEmpty();
    }
    @Test void pendingLimitCapDoesNotPreventImmediateOrders() {
        for(int i=0;i<25;i++) assertThat(place(OrderSide.BUY,OrderType.LIMIT,1,"90").status()).isEqualTo(OrderStatus.PENDING);
        assertThat(place(OrderSide.BUY,OrderType.LIMIT,1,"90").reason()).contains("PENDING_LIMIT");
        assertThat(place(OrderSide.BUY,OrderType.MARKET,1,null).status()).isEqualTo(OrderStatus.EXECUTED);
    }
    @Test void expiredOrdersDoNotConsumeNextSessionFundsOrCapacity() {
        for(int i=0;i<25;i++) place(OrderSide.BUY,OrderType.LIMIT,1,"90");
        clock.set("2026-11-02T04:30:00Z"); assertThat(place(OrderSide.BUY,OrderType.LIMIT,1,"90").status()).isEqualTo(OrderStatus.PENDING);
        assertThat(account().blockedBalance()).isEqualByComparingTo("90.06");
    }
    @Test void blockedAccountMaySellButCannotBuy() {
        flags(true,true); assertThat(place(OrderSide.BUY,OrderType.MARKET,1,null).reason()).contains("ACCOUNT_BLOCKED");
        assertThat(place(OrderSide.SELL,OrderType.MARKET,1,null).status()).isEqualTo(OrderStatus.EXECUTED);
    }
    @ParameterizedTest @EnumSource(OrderSide.class)
    void unapprovedKycRejectsBothSides(OrderSide side) { flags(false,false); assertThat(place(side,OrderType.MARKET,1,null).reason()).contains("KYC_REQUIRED"); }
    @Test void suspendedStockRejectsNewAndPendingOrdersAndReleasesResources() {
        var o=place(OrderSide.SELL,OrderType.LIMIT,2,"110"); suspended(); trading.processOrder(o.id());
        assertThat(orders.get(USER,o.id()).status()).isEqualTo(OrderStatus.REJECTED); assertThat(holding().reservedQuantity()).isZero();
        assertThat(place(OrderSide.BUY,OrderType.MARKET,1,null).reason()).contains("STOCK_SUSPENDED"); assertThat(holding().quantity()).isEqualTo(10);
    }
    @Test void accountRestrictionsAreRecheckedBeforePendingExecution() {
        var o=place(OrderSide.BUY,OrderType.LIMIT,1,"90"); flags(true,true); price("80"); trading.processOrder(o.id());
        assertThat(orders.get(USER,o.id()).status()).isEqualTo(OrderStatus.REJECTED); assertThat(account().blockedBalance()).isZero();
    }
    @ParameterizedTest @CsvSource({"2026-10-30T03:44:59Z,false","2026-10-30T03:45:00Z,true","2026-10-30T09:59:59Z,true","2026-10-30T10:00:00Z,false","2026-10-31T04:30:00Z,false","2026-11-01T04:30:00Z,false"})
    void marketHoursUseIstAndWeekdays(String instant,boolean open) {
        clock.set(instant); assertThat(market.isOpen(clock.instant())).isEqualTo(open);
        assertThat(place(OrderSide.BUY,OrderType.MARKET,1,null).status()).isEqualTo(open?OrderStatus.EXECUTED:OrderStatus.REJECTED);
    }
    @ParameterizedTest @ValueSource(ints={-1,0,1001})
    void invalidQuantitiesNeverCreateOrders(int qty) {
        assertThatThrownBy(()->place(OrderSide.BUY,OrderType.MARKET,qty,null)).isInstanceOf(TradingException.class);
        assertThat(orders.history(USER,USER)).isEmpty();
    }
    @Test void invalidLimitPriceAndMarketPriceFieldAreRejectedBeforeCreatingIntent() {
        for(var r:List.of(request(OrderSide.BUY,OrderType.LIMIT,1,null),request(OrderSide.BUY,OrderType.LIMIT,1,"0"),
                request(OrderSide.BUY,OrderType.MARKET,1,"100"),new PlaceOrderRequest("TCS",OrderSide.BUY,OrderType.LIMIT,1,new BigDecimal("10.001"))))
            assertThatThrownBy(()->trading.place(USER,r,null)).isInstanceOf(TradingException.class);
        assertThat(orders.history(USER,USER)).isEmpty();
    }
    @Test void unknownStockCreatesRejectedIntentButUnknownUserDoesNot() {
        var r=new PlaceOrderRequest("UNKNOWN",OrderSide.BUY,OrderType.MARKET,1,null);
        assertThat(trading.place(USER,r,null).status()).isEqualTo(OrderStatus.REJECTED);
        assertThatThrownBy(()->trading.place(UUID.randomUUID(),r,null)).isInstanceOf(TradingException.class);
    }
    @Test void previewIsAuthoritativeButDoesNotReserveOrCreateRecords() {
        var p=trading.preview(USER,request(OrderSide.BUY,OrderType.LIMIT,10,"90"));
        assertThat(p.cashToReserve()).isEqualByComparingTo("900.53"); assertThat(p.executesImmediately()).isFalse();
        assertThat(account().availableBalance()).isEqualByComparingTo("100000"); assertThat(orders.history(USER,USER)).isEmpty();
    }
    @Test void repeatedProcessingCreatesOnlyOneTrade() {
        var o=place(OrderSide.BUY,OrderType.LIMIT,1,"90"); price("80");
        for(int i=0;i<10;i++) trading.processOrder(o.id()); assertThat(trades()).hasSize(1); assertThat(holding().quantity()).isEqualTo(11);
    }
    @Test void settlementFailureRollsBackCashHoldingOrderAndTrade() {
        doAnswer(invocation->{ invocation.callRealMethod(); throw new IllegalStateException("Injected trade persistence failure"); }).when(tradeRepository).save(any());
        assertThatThrownBy(()->place(OrderSide.BUY,OrderType.MARKET,2,null)).isInstanceOf(IllegalStateException.class);
        assertThat(account().availableBalance()).isEqualByComparingTo("100000"); assertThat(account().blockedBalance()).isZero();
        assertThat(holding().quantity()).isEqualTo(10); assertThat(orders.history(USER,USER)).isEmpty(); assertThat(trades()).isEmpty();
    }
    @Test void failedPendingSettlementRetainsOriginalReservationAndCanRetry() {
        var o=place(OrderSide.BUY,OrderType.LIMIT,2,"90"); price("80");
        doThrow(new IllegalStateException("Injected settlement failure")).when(accounts).settleBuy(any(),any(),anyInt(),any(),any(),any());
        assertThatThrownBy(()->trading.processOrder(o.id())).isInstanceOf(IllegalStateException.class);
        assertThat(orders.get(USER,o.id()).status()).isEqualTo(OrderStatus.PENDING); assertThat(account().blockedBalance()).isEqualByComparingTo(o.reservedCash());
        assertThat(holding().quantity()).isEqualTo(10); assertThat(trades()).isEmpty();
        doCallRealMethod().when(accounts).settleBuy(any(),any(),anyInt(),any(),any(),any()); trading.processOrder(o.id()); assertThat(trades()).hasSize(1);
    }
    @Test void idempotencyReplaysOneIntentAndRejectsDifferentPayload() {
        var r=request(OrderSide.BUY,OrderType.MARKET,1,null); var first=trading.place(USER,r,"retry-key");
        assertThat(trading.place(USER,r,"retry-key").id()).isEqualTo(first.id());
        assertThatThrownBy(()->trading.place(USER,request(OrderSide.BUY,OrderType.MARKET,2,null),"retry-key")).isInstanceOf(TradingException.class);
        assertThat(trades()).hasSize(1);
    }
    @Test void ownershipChecksProtectDetailCancellationAndHistories() {
        var o=place(OrderSide.BUY,OrderType.LIMIT,1,"90"); var other=UUID.randomUUID();
        assertThatThrownBy(()->orders.get(other,o.id())).isInstanceOf(TradingException.class);
        assertThatThrownBy(()->orders.cancel(other,o.id())).isInstanceOf(TradingException.class);
        assertThatThrownBy(()->orders.history(other,USER)).isInstanceOf(TradingException.class);
        assertThatThrownBy(()->orders.trades(other,USER)).isInstanceOf(TradingException.class);
    }
    @Test void concurrentBuysCannotOverspend() throws Exception {
        balance("1000"); var results=race(32,()->place(OrderSide.BUY,OrderType.MARKET,6,null));
        assertThat(results.stream().filter(o->o.status()==OrderStatus.EXECUTED)).hasSize(1);
        assertThat(account().availableBalance()).isEqualByComparingTo("399.65"); assertThat(trades()).hasSize(1);
    }
    @Test void concurrentSellsCannotReserveSameShares() throws Exception {
        var results=race(32,()->place(OrderSide.SELL,OrderType.LIMIT,6,"110"));
        assertThat(results.stream().filter(o->o.status()==OrderStatus.PENDING)).hasSize(1);
        assertThat(holding().reservedQuantity()).isEqualTo(6); assertThat(holding().availableQuantity()).isEqualTo(4);
    }
    @Test void concurrentPendingCapacityCheckIsAtomic() throws Exception {
        var results=race(48,()->place(OrderSide.BUY,OrderType.LIMIT,1,"90"));
        assertThat(results.stream().filter(o->o.status()==OrderStatus.PENDING)).hasSize(25);
        assertThat(account().blockedBalance()).isEqualByComparingTo("2251.50");
    }
    @Test void concurrentIdempotentRetriesHaveOneExecution() throws Exception {
        var results=race(48,()->trading.place(USER,request(OrderSide.BUY,OrderType.MARKET,1,null),"concurrent-retry"));
        assertThat(results.stream().map(OrderResponse::id).distinct()).hasSize(1); assertThat(trades()).hasSize(1);
    }
    @Test void executionCancellationRaceHasOneTerminalOutcome() throws Exception {
        var o=place(OrderSide.BUY,OrderType.LIMIT,2,"90"); price("80");
        race(48,()->{ if(ThreadLocalRandom.current().nextBoolean()) trading.processOrder(o.id()); else try { orders.cancel(USER,o.id()); } catch(TradingException expected) { /* execution or another cancellation won */ } return true; });
        var finalOrder=orders.get(USER,o.id()); assertThat(finalOrder.status()).isIn(OrderStatus.EXECUTED,OrderStatus.CANCELLED);
        assertThat(account().blockedBalance()).isZero();
        if(finalOrder.status()==OrderStatus.EXECUTED) { assertThat(trades()).hasSize(1); assertThat(holding().quantity()).isEqualTo(12); assertThat(account().availableBalance()).isEqualByComparingTo("99839.91"); }
        else { assertThat(trades()).isEmpty(); assertThat(holding().quantity()).isEqualTo(10); assertThat(account().availableBalance()).isEqualByComparingTo("100000"); }
    }
    @Test void repositoryAccessOutsideTransactionIsRejected() { assertThatThrownBy(()->orderRepository.pending()).isInstanceOf(IllegalStateException.class); }
    @Test void sellerNeedsSharesButDoesNotNeedExistingCashForFees() {
        balance("0"); assertThat(place(OrderSide.SELL,OrderType.MARKET,1,null).status()).isEqualTo(OrderStatus.EXECUTED);
        assertThat(account().availableBalance()).isEqualByComparingTo("99.94");
    }
    @Test void wholeBookInvariantsHoldAcrossMixedOrderLifecycles() {
        var random=new Random(20261030);
        for(int step=0;step<200;step++) {
            switch(random.nextInt(5)) {
                case 0,1 -> {
                    var type=random.nextBoolean()?OrderType.MARKET:OrderType.LIMIT;
                    place(random.nextBoolean()?OrderSide.BUY:OrderSide.SELL,type,random.nextInt(4)+1,type==OrderType.LIMIT?(random.nextBoolean()?"90":"110"):null);
                }
                case 2 -> {
                    var pending=orders.history(USER,USER).stream().filter(o->o.status()==OrderStatus.PENDING).toList();
                    if(!pending.isEmpty()) orders.cancel(USER,pending.get(random.nextInt(pending.size())).id());
                }
                case 3 -> { price(String.valueOf(85+random.nextInt(31))); trading.processPending(); }
                default -> trading.processPending();
            }
            assertBookInvariants();
        }
    }
    private void assertBookInvariants() {
        var history=orders.history(USER,USER);
        var pending=history.stream().filter(o->o.status()==OrderStatus.PENDING).toList();
        assertThat(account().blockedBalance()).isEqualByComparingTo(pending.stream().map(OrderResponse::reservedCash).reduce(Money.ZERO,BigDecimal::add));
        assertThat(holding().reservedQuantity()).isEqualTo(pending.stream().mapToInt(OrderResponse::reservedShares).sum());
        assertThat(account().availableBalance().signum()).isGreaterThanOrEqualTo(0);
        assertThat(holding().availableQuantity()).isGreaterThanOrEqualTo(0);
        var ledger=trades();
        assertThat(ledger.stream().map(Trade::orderId).distinct().count()).isEqualTo((long)ledger.size());
        assertThat(holding().quantity()).isEqualTo(10+ledger.stream().mapToInt(t->t.side()==OrderSide.BUY?t.quantity():-t.quantity()).sum());
        assertThat(account().availableBalance().add(account().blockedBalance())).isEqualByComparingTo(ledger.stream().map(Trade::netCashFlow).reduce(bd("100000"),BigDecimal::add));
        assertThat(ledger.size()).isEqualTo(history.stream().filter(o->o.status()==OrderStatus.EXECUTED).count());
    }
    <T> List<T> race(int count,Supplier<T> operation) throws Exception {
        try(var pool=Executors.newFixedThreadPool(12)) {
            var start=new CountDownLatch(1); var futures=new ArrayList<Future<T>>();
            for(int i=0;i<count;i++) futures.add(pool.submit(()->{start.await();return operation.get();}));
            start.countDown(); var result=new ArrayList<T>(); for(var future:futures) result.add(future.get(15,TimeUnit.SECONDS)); return result;
        }
    }
    static class MutableClock extends Clock {
        private final AtomicReference<Instant> instant;
        MutableClock(String value) { instant=new AtomicReference<>(Instant.parse(value)); }
        void set(String value) { instant.set(Instant.parse(value)); }
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return Clock.fixed(instant(),zone); }
        public Instant instant() { return instant.get(); }
    }
}
