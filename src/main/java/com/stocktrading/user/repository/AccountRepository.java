package com.stocktrading.user.repository;

import com.stocktrading.user.model.Account;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory thread-safe repository for Account records.
 */
@Repository
public class AccountRepository {

    private final Map<String, Account> accountMap = new ConcurrentHashMap<>();
    private final Map<String, String> userIdToAccountIdIndex = new ConcurrentHashMap<>();

    public Account save(Account account) {
        if (account == null || account.getAccountId() == null) {
            throw new IllegalArgumentException("Account and Account ID cannot be null");
        }
        accountMap.put(account.getAccountId(), account);
        if (account.getUserId() != null) {
            userIdToAccountIdIndex.put(account.getUserId(), account.getAccountId());
        }
        return account;
    }

    public Optional<Account> findById(String accountId) {
        if (accountId == null) return Optional.empty();
        return Optional.ofNullable(accountMap.get(accountId));
    }

    public Optional<Account> findByUserId(String userId) {
        if (userId == null) return Optional.empty();
        String accountId = userIdToAccountIdIndex.get(userId);
        if (accountId == null) {
            // fallback scan in case index was not updated
            return accountMap.values().stream()
                    .filter(acc -> userId.equals(acc.getUserId()))
                    .findFirst();
        }
        return findById(accountId);
    }

    public List<Account> findAll() {
        return new ArrayList<>(accountMap.values());
    }

    public boolean existsById(String accountId) {
        return accountId != null && accountMap.containsKey(accountId);
    }

    public boolean existsByUserId(String userId) {
        return userId != null && userIdToAccountIdIndex.containsKey(userId);
    }

    public void clear() {
        accountMap.clear();
        userIdToAccountIdIndex.clear();
    }
}

