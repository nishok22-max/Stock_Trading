package com.stocktrading.user.model;

import java.time.LocalDateTime;

/**
 * Base User entity demonstrating Encapsulation and Inheritance.
 * Contains user ID, name, email, password hash, PAN, and contact details.
 */
public abstract class User {
    private String userId;
    private String name;
    private String email;
    private String passwordHash;
    private String panNumber;
    private String phone;
    private UserRole role;
    private LocalDateTime createdAt;

    public User() {
        this.createdAt = LocalDateTime.now();
    }

    public User(String userId, String name, String email, String passwordHash, 
                String panNumber, String phone, UserRole role) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.panNumber = panNumber;
        this.phone = phone;
        this.role = role;
        this.createdAt = LocalDateTime.now();
    }

    // Encapsulation: Getter and Setter methods
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPanNumber() {
        return panNumber;
    }

    public void setPanNumber(String panNumber) {
        this.panNumber = panNumber;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Abstract method demonstrating polymorphism across user types.
     */
    public abstract String getUserTypeDescription();

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", panNumber='" + panNumber + '\'' +
                ", phone='" + phone + '\'' +
                ", role=" + role +
                '}';
    }
}
