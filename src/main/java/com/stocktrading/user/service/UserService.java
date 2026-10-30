package com.stocktrading.user.service;

import com.stocktrading.user.dto.RegisterRequest;
import com.stocktrading.user.exception.UserAlreadyExistsException;
import com.stocktrading.user.exception.UserNotFoundException;
import com.stocktrading.user.model.Admin;
import com.stocktrading.user.model.Customer;
import com.stocktrading.user.model.User;
import com.stocktrading.user.model.UserRole;
import com.stocktrading.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service managing User entities.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final AtomicLong userIdSequence = new AtomicLong(1100);

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String generateUserId() {
        return "USR-" + userIdSequence.incrementAndGet();
    }

    public Customer createCustomer(RegisterRequest request, String passwordHash) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("A user with email " + request.getEmail() + " already exists");
        }
        if (userRepository.existsByPanNumber(request.getPanNumber())) {
            throw new UserAlreadyExistsException("A user with PAN number " + request.getPanNumber().toUpperCase() + " already exists");
        }

        String userId = generateUserId();
        Customer customer = new Customer(
                userId,
                request.getName().trim(),
                request.getEmail().trim().toLowerCase(),
                passwordHash,
                request.getPanNumber().trim().toUpperCase(),
                request.getPhone().trim(),
                request.getLinkedBankAccount() != null ? request.getLinkedBankAccount().trim() : "HDFC-" + (100000 + (long)(Math.random() * 900000))
        );

        userRepository.save(customer);
        return customer;
    }

    public Admin createAdmin(String name, String email, String passwordHash, 
                            String panNumber, String phone, String department) {
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("A user with email " + email + " already exists");
        }

        String userId = generateUserId();
        Admin admin = new Admin(
                userId,
                name.trim(),
                email.trim().toLowerCase(),
                passwordHash,
                panNumber.trim().toUpperCase(),
                phone.trim(),
                department
        );

        userRepository.save(admin);
        return admin;
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean existsByPanNumber(String panNumber) {
        return userRepository.existsByPanNumber(panNumber);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
