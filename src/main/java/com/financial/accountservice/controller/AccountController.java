package com.financial.accountservice.controller;

import com.financial.accountservice.client.UserServiceClient;
import com.financial.accountservice.entity.Account;
import com.financial.accountservice.repository.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountRepository accountRepository;
    private final UserServiceClient userServiceClient;

    public AccountController(AccountRepository accountRepository, UserServiceClient userServiceClient) {
        this.accountRepository = accountRepository;
        this.userServiceClient = userServiceClient;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("service", "account-service");
        response.put("status", "UP");
        return response;
    }

    @PostMapping
    public ResponseEntity<?> createAccount(@RequestBody Account account) {
        // Validate user exists via User Service
        try {
            Object user = userServiceClient.getUserById(account.getUserId());
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "User service unavailable"));
        }

        // Generate account number
        account.setAccountNumber("ACC-" + UUID.randomUUID().toString().substring(0, 8));
        if (account.getBalance() == null) account.setBalance(BigDecimal.ZERO);

        Account saved = accountRepository.save(account);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable Long id) {
        return accountRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public List<Account> getAccountsByUser(@PathVariable Long userId) {
        return accountRepository.findByUserId(userId);
    }

    @PostMapping("/{id}/debit")
    public ResponseEntity<?> debit(@PathVariable Long id, @RequestBody Map<String, BigDecimal> request) {
        BigDecimal amount = request.get("amount");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid amount"));
        }

        Account account = accountRepository.findById(id).orElse(null);
        if (account == null) return ResponseEntity.notFound().build();

        if (account.getBalance().compareTo(amount) < 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Insufficient balance"));
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
        return ResponseEntity.ok(account);
    }

    @PostMapping("/{id}/credit")
    public ResponseEntity<?> credit(@PathVariable Long id, @RequestBody Map<String, BigDecimal> request) {
        BigDecimal amount = request.get("amount");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid amount"));
        }

        Account account = accountRepository.findById(id).orElse(null);
        if (account == null) return ResponseEntity.notFound().build();

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        return ResponseEntity.ok(account);
    }
}