package com.microservices.log430.orderservice.adapters.external.notification;

import com.microservices.log430.orderservice.adapters.external.config.GlobalFeignConfig;
import com.microservices.log430.orderservice.adapters.external.notification.dto.NotificationLogDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", url = "${gateway.url:http://api-gateway:8079}", configuration = GlobalFeignConfig.class)
public interface NotificationClient {
    @PostMapping("/api/v1/notifications")
    void sendNotification(@RequestBody NotificationLogDTO notificationLogDTO);
}

