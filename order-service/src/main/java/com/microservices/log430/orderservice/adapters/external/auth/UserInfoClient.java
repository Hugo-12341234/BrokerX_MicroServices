package com.microservices.log430.orderservice.adapters.external.auth;

import com.microservices.log430.orderservice.adapters.external.auth.dto.UserDTO;
import com.microservices.log430.orderservice.adapters.external.config.GlobalFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", url = "${gateway.url:http://api-gateway:8079}", configuration = GlobalFeignConfig.class)
public interface UserInfoClient {
    @GetMapping("/api/v1/users/{userId}")
    UserDTO getUserInfo(@PathVariable("userId") Long userId);
}
