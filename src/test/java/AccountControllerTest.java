package com.financial.accountservice.controller;

import com.financial.accountservice.client.UserServiceClient;
import com.financial.accountservice.entity.Account;
import com.financial.accountservice.repository.AccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountRepository accountRepository;

    @MockBean
    private UserServiceClient userServiceClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void health_ShouldReturnUp() throws Exception {
        mockMvc.perform(get("/api/accounts/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void createAccount_ShouldReturnCreated() throws Exception {
        Account input = new Account(null, 1L, null, BigDecimal.ZERO, "SAVINGS");
        Account saved = new Account(1L, 1L, "ACC-abc123", BigDecimal.ZERO, "SAVINGS");

        when(userServiceClient.getUserById(1L)).thenReturn(new Object()); // successful user check
        when(accountRepository.save(any(Account.class))).thenReturn(saved);

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.accountNumber", notNullValue()));
    }

    @Test
    void getAccount_WhenExists() throws Exception {
        Account acc = new Account(1L, 1L, "ACC-abc", new BigDecimal("500.00"), "CHECKING");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(acc));

        mockMvc.perform(get("/api/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(500.00)));
    }

    @Test
    void debit_ShouldSucceed() throws Exception {
        Account acc = new Account(1L, 1L, "ACC-abc", new BigDecimal("500.00"), "CHECKING");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(acc));
        when(accountRepository.save(any(Account.class))).thenReturn(acc);

        Map<String, BigDecimal> body = Collections.singletonMap("amount", new BigDecimal("100.00"));
        mockMvc.perform(post("/api/accounts/1/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(400.00)));
    }

    @Test
    void debit_InsufficientBalance() throws Exception {
        Account acc = new Account(1L, 1L, "ACC-abc", new BigDecimal("50.00"), "CHECKING");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(acc));

        Map<String, BigDecimal> body = Collections.singletonMap("amount", new BigDecimal("100.00"));
        mockMvc.perform(post("/api/accounts/1/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void credit_ShouldSucceed() throws Exception {
        Account acc = new Account(1L, 1L, "ACC-abc", new BigDecimal("200.00"), "CHECKING");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(acc));
        when(accountRepository.save(any(Account.class))).thenReturn(acc);

        Map<String, BigDecimal> body = Collections.singletonMap("amount", new BigDecimal("50.00"));
        mockMvc.perform(post("/api/accounts/1/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(250.00)));
    }
}