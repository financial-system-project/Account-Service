package com.financial.accountservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${user.service.url:http://user-service:8080}")
public interface UserServiceClient {

    @GetMapping("/api/users/{id}")
    Object getUserById(@PathVariable("id") Long id);
}