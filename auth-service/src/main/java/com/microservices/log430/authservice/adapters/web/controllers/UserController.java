package com.microservices.log430.authservice.adapters.web.controllers;

import com.microservices.log430.authservice.adapters.web.dto.UserForm;
import com.microservices.log430.authservice.domain.port.in.RegistrationPort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/users")
public class UserController {
    private final RegistrationPort registrationPort;

    public UserController(RegistrationPort registrationPort) {
        this.registrationPort = registrationPort;
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("userForm", new UserForm());
        return "user/register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("userForm") UserForm form, Model model) {
        if ((form.getEmail() == null || form.getEmail().isBlank())) {
            model.addAttribute("error", "Veuillez renseigner un email.");
            return "user/register";
        }
        // Validation format email
        if (form.getEmail() != null && !form.getEmail().isBlank() &&
                !form.getEmail().matches("^.+@.+\\..+$")) {
            model.addAttribute("error", "Format d'email invalide.");
            return "user/register";
        }
        // Validation date de naissance
        if (form.getDateDeNaissance() == null) {
            model.addAttribute("error", "Veuillez renseigner la date de naissance.");
            return "user/register";
        }

        try {
            registrationPort.register(
                    form.getEmail(),
                    form.getPassword(),
                    form.getName(),
                    form.getAdresse(),
                    form.getDateDeNaissance()
            );
            return "redirect:/users/success";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "user/register";
        }
    }

    @GetMapping("/success")
    public String registrationSuccess() {
        return "user/success";
    }
}