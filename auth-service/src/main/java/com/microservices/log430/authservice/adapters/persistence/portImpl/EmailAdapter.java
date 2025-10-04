package com.microservices.log430.authservice.adapters.persistence.portImpl;

import com.microservices.log430.authservice.domain.port.out.EmailSenderPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@Component
public class EmailAdapter implements EmailSenderPort {
    private final JavaMailSender mailSender;

    @Autowired
    public EmailAdapter(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
