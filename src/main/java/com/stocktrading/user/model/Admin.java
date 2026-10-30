package com.stocktrading.user.model;

/**
 * Admin entity extending User with administrative permissions (KYC approval, account blocking).
 */
public class Admin extends User {
    private String department;

    public Admin() {
        super();
        setRole(UserRole.ADMIN);
    }

    public Admin(String userId, String name, String email, String passwordHash, 
                 String panNumber, String phone, String department) {
        super(userId, name, email, passwordHash, panNumber, phone, UserRole.ADMIN);
        this.department = department;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    @Override
    public String getUserTypeDescription() {
        return "Trading Platform Administrator";
    }
}
