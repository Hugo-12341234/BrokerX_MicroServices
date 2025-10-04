package com.microservices.log430.authservice.domain.port.out;

public interface EmailSenderPort {
    void sendEmail(String to, String subject, String body);
}