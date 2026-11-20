package com.example.lab4.dto;

import com.example.lab4.model.Role;
import com.example.lab4.model.UserStatus;

public record UserRequest(String username, String password, String name, String email, Role role,
                          UserStatus status) {
}
