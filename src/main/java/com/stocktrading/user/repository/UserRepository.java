package com.stocktrading.user.repository;

import com.stocktrading.user.model.User;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory thread-safe repository for User records.
 */
@Repository
public class UserRepository {

    private final Map<String, User> userMap = new ConcurrentHashMap<>();
    private final Map<String, String> emailToIdIndex = new ConcurrentHashMap<>();
    private final Map<String, String> panToIdIndex = new ConcurrentHashMap<>();

    public User save(User user) {
        if (user == null || user.getUserId() == null) {
            throw new IllegalArgumentException("User and User ID cannot be null");
        }
        userMap.put(user.getUserId(), user);
        if (user.getEmail() != null) {
            emailToIdIndex.put(user.getEmail().toLowerCase(), user.getUserId());
        }
        if (user.getPanNumber() != null) {
            panToIdIndex.put(user.getPanNumber().toUpperCase(), user.getUserId());
        }
        return user;
    }

    public Optional<User> findById(String userId) {
        if (userId == null) return Optional.empty();
        return Optional.ofNullable(userMap.get(userId));
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        String userId = emailToIdIndex.get(email.toLowerCase());
        if (userId == null) return Optional.empty();
        return findById(userId);
    }

    public Optional<User> findByPanNumber(String panNumber) {
        if (panNumber == null) return Optional.empty();
        String userId = panToIdIndex.get(panNumber.toUpperCase());
        if (userId == null) return Optional.empty();
        return findById(userId);
    }

    public boolean existsByEmail(String email) {
        if (email == null) return false;
        return emailToIdIndex.containsKey(email.toLowerCase());
    }

    public boolean existsByPanNumber(String panNumber) {
        if (panNumber == null) return false;
        return panToIdIndex.containsKey(panNumber.toUpperCase());
    }

    public List<User> findAll() {
        return new ArrayList<>(userMap.values());
    }

    public long count() {
        return userMap.size();
    }

    public void clear() {
        userMap.clear();
        emailToIdIndex.clear();
        panToIdIndex.clear();
    }
}
