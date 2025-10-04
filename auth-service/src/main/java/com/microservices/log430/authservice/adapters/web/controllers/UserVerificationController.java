package com.microservices.log430.authservice.adapters.web.controllers;

import com.microservices.log430.authservice.domain.port.in.RegistrationPort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserVerificationController {
    private final RegistrationPort registrationPort;

    public UserVerificationController(RegistrationPort registrationPort) {
        this.registrationPort = registrationPort;
    }

    @GetMapping("/verify")
    public String verify(@RequestParam String token, Model model) {
        boolean success = registrationPort.verifyUser(token);
        model.addAttribute("success", success);
        return "user/verification_result";
    }
}
