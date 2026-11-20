package com.example.lab4.controller;

import com.example.lab4.dto.TokenPair;
import com.example.lab4.dto.UserRequest;
import com.example.lab4.model.Role;
import com.example.lab4.model.UserStatus;
import com.example.lab4.service.AuthService;
import com.example.lab4.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
public class AuthController {
    @Autowired private AuthService auth;
    @Autowired private UserService users;

    public record LoginRequest(String username, String password, String deviceId) {}
    public record RefreshRequest(String refreshToken) {}
    public record RegisterRequest(String username, String password, String name, String email,
                                  Role role, UserStatus status, String deviceId) {}

    @PostMapping("/auth/login")
    public TokenPair login(@RequestBody LoginRequest request) {
        return auth.login(request.username(), request.password(), request.deviceId());
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenPair register(@RequestBody RegisterRequest r) {
        return auth.register(new UserRequest(r.username(), r.password(), r.name(), r.email(), r.role(), r.status()),
                r.deviceId());
    }

    @PostMapping("/auth/refresh")
    public TokenPair refresh(@RequestBody RefreshRequest request) {
        TokenPair pair = auth.refresh(request.refreshToken());
        if (pair == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh уже использован");
        return pair;
    }

    @GetMapping("/api/auth/me")
    public Map<String, Object> me() {
        return UserController.toJson(users.getCurrentUser());
    }
}
