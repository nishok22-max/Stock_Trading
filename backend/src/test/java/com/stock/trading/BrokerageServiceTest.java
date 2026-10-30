package com.stock.trading;

import com.stock.trading.service.BrokerageService;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;

class BrokerageServiceTest {
    @ParameterizedTest @CsvSource({"100,10,0.50,0.09,0.59","400,100,20.00,3.60,23.60","1000,1000,20.00,3.60,23.60","0.01,1,0.00,0.00,0.00","10,1,0.01,0.00,0.01","85,10,0.43,0.08,0.51","90,1,0.05,0.01,0.06"})
    void brokerageCapGstAndPaiseRounding(String price,int quantity,String brokerage,String gst,String total) {
        var fee=new BrokerageService().calculate(new BigDecimal(price),quantity);
        assertThat(fee.brokerage()).isEqualByComparingTo(brokerage);
        assertThat(fee.gst()).isEqualByComparingTo(gst);
        assertThat(fee.totalCharges()).isEqualByComparingTo(total);
        assertThat(fee.buyTotal().subtract(fee.value())).isEqualByComparingTo(total);
        assertThat(fee.value().subtract(fee.sellProceeds())).isEqualByComparingTo(total);
    }
}
