package com.stocktrading.user.model;

/**
 * Customer entity extending User.
 */
public class Customer extends User {
    private String bankAccountNumber;

    public Customer() {
        super();
        setRole(UserRole.CUSTOMER);
    }

    public Customer(String userId, String name, String email, String passwordHash, 
                    String panNumber, String phone, String bankAccountNumber) {
        super(userId, name, email, passwordHash, panNumber, phone, UserRole.CUSTOMER);
        this.bankAccountNumber = bankAccountNumber;
    }

    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }

    @Override
    public String getUserTypeDescription() {
        return "Retail Trading Customer";
    }
}

