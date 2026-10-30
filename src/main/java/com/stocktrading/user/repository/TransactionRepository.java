package com.stocktrading.user.repository;

import com.stocktrading.user.model.Transaction;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * In-memory thread-safe repository for Transaction records.
 */
@Repository
public class TransactionRepository {

    private final Map<String, Transaction> transactionMap = new ConcurrentHashMap<>();
    private final Map<String, List<Transaction>> userTransactionsIndex = new ConcurrentHashMap<>();
    private final Map<String, List<Transaction>> accountTransactionsIndex = new ConcurrentHashMap<>();

    private final Comparator<Transaction> transactionComparator = 
            Comparator.comparing(Transaction::getTimestamp)
                      .thenComparing(Transaction::getTransactionId)
                      .reversed();

    public Transaction save(Transaction transaction) {
        if (transaction == null || transaction.getTransactionId() == null) {
            throw new IllegalArgumentException("Transaction and Transaction ID cannot be null");
        }
        transactionMap.put(transaction.getTransactionId(), transaction);

        if (transaction.getUserId() != null) {
            userTransactionsIndex.computeIfAbsent(transaction.getUserId(), k -> new CopyOnWriteArrayList<>())
                    .add(transaction);
        }
        if (transaction.getAccountId() != null) {
            accountTransactionsIndex.computeIfAbsent(transaction.getAccountId(), k -> new CopyOnWriteArrayList<>())
                    .add(transaction);
        }

        return transaction;
    }

    public Optional<Transaction> findById(String transactionId) {
        if (transactionId == null) return Optional.empty();
        return Optional.ofNullable(transactionMap.get(transactionId));
    }

    public List<Transaction> findByUserId(String userId) {
        if (userId == null) return new ArrayList<>();
        List<Transaction> list = userTransactionsIndex.getOrDefault(userId, new ArrayList<>());
        return list.stream()
                .sorted(transactionComparator)
                .collect(Collectors.toList());
    }

    public List<Transaction> findByAccountId(String accountId) {
        if (accountId == null) return new ArrayList<>();
        List<Transaction> list = accountTransactionsIndex.getOrDefault(accountId, new ArrayList<>());
        return list.stream()
                .sorted(transactionComparator)
                .collect(Collectors.toList());
    }

    public List<Transaction> findAll() {
        return transactionMap.values().stream()
                .sorted(transactionComparator)
                .collect(Collectors.toList());
    }

    public void clear() {
        transactionMap.clear();
        userTransactionsIndex.clear();
        accountTransactionsIndex.clear();
    }
}
