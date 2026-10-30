package com.stocktrading.user.dto;

import com.stocktrading.user.model.UserRole;

/**
 * DTO for Login response.
 */
public class LoginResponse {
    private String token;
    private String message;
    private String userId;
    private String name;
    private String email;
    private UserRole role;
    private AccountResponse account;

    public LoginResponse() {
    }

    public LoginResponse(String token, String message, String userId, String name, 
                         String email, UserRole role, AccountResponse account) {
        this.token = token;
        this.message = message;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.account = account;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public AccountResponse getAccount() {
        return account;
    }

    public void setAccount(AccountResponse account) {
        this.account = account;
    }
}

