package com.microservices.log430.orderservice.adapters.web.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

@Controller
@RequestMapping("/orders")
public class OrderController {
    private final OrderService orderService;
    private final UserPort userPort;
    private final AuthenticationPort authenticationPort;
    private final JwtTokenPort jwtTokenPort;

    @Autowired
    public OrderController(OrderService orderService,
                           UserPort userPort,
                           AuthenticationPort authenticationPort,
                           JwtTokenPort jwtTokenPort) {
        this.orderService = orderService;
        this.userPort = userPort;
        this.authenticationPort = authenticationPort;
        this.jwtTokenPort = jwtTokenPort;
    }

    @GetMapping("/place")
    public String showOrderForm(Model model) {
        model.addAttribute("orderRequest", new OrderRequest());
        return "order/place_order";
    }

    @PostMapping("/place")
    public String placeOrder(@ModelAttribute OrderRequest orderRequest, Model model, HttpServletRequest httpRequest) {
        String jwtTokenString = getJwtFromRequest(httpRequest);
        OrderResponse orderResponse;
        if (jwtTokenString == null || !authenticationPort.validateToken(jwtTokenString)) {
            orderResponse = new OrderResponse(null, "REJETE", "Non authentifié");
            model.addAttribute("orderResponse", orderResponse);
            model.addAttribute("error", orderResponse.getMessage());
            return "order/order_result";
        }
        try {
            Long userId = jwtTokenPort.getUserIdFromToken(jwtTokenString);
            Optional<User> userOpt = userPort.findById(userId);
            if (userOpt.isEmpty()) {
                orderResponse = new OrderResponse(null, "REJETE", "Utilisateur non trouvé");
                model.addAttribute("orderResponse", orderResponse);
                model.addAttribute("error", orderResponse.getMessage());
                return "order/order_result";
            }
            orderResponse = orderService.placeOrder(orderRequest, userId);
            model.addAttribute("orderResponse", orderResponse);
            if ("ACCEPTE".equals(orderResponse.getStatus())) {
                model.addAttribute("success", orderResponse.getMessage());
            } else {
                model.addAttribute("error", orderResponse.getMessage());
            }
            return "order/order_result";
        } catch (Exception e) {
            orderResponse = new OrderResponse(null, "REJETE", "Erreur lors du placement de l'ordre : " + e.getMessage());
            model.addAttribute("orderResponse", orderResponse);
            model.addAttribute("error", orderResponse.getMessage());
            return "order/order_result";
        }
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        // Chercher d'abord dans les attributs de session (pour les tests)
        Object sessionJwt = request.getSession().getAttribute("jwtToken");
        if (sessionJwt instanceof String) {
            return (String) sessionJwt;
        }

        // Chercher dans les cookies
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // Fallback: chercher dans l'en-tête Authorization
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}
