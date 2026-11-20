package com.example.lab4.service;

import com.example.lab4.dto.UserRequest;
import com.example.lab4.model.Role;
import com.example.lab4.model.User;
import com.example.lab4.model.UserStatus;
import com.example.lab4.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<User> getAll() {
        return userRepository.findAll();
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));
    }

    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Пользователь не найден"));
    }

    public User register(UserRequest request) {
        return createUser(request, Role.CUSTOMER);
    }

    public User create(UserRequest request) {
        if (request.role() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не указана роль");
        return createUser(request, request.role());
    }

    public User update(Long id, UserRequest request) {
        User user = getById(id);
        boolean itsMe = user.getId().equals(getCurrentUser().getId());
        if (request.username() != null && !request.username().equals(user.getUsername()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Логин менять нельзя");
        if (itsMe && ((request.role() != null && request.role() != user.getRole()) || request.status() == UserStatus.BLOCKED))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Нельзя менять роль или блокировать самого себя");
        if (request.name() != null && request.name().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Имя не может быть пустым");
        if (request.email() != null && !request.email().equals(user.getEmail())
                && userRepository.existsByEmail(request.email()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Пользователь с таким email уже есть");
        if (request.password() != null) checkPassword(request.password());

        if (request.name() != null) user.setName(request.name());
        if (request.email() != null) user.setEmail(request.email());
        if (request.role() != null) user.setRole(request.role());
        if (request.status() != null) user.setStatus(request.status());
        if (request.password() != null) user.setPassword(passwordEncoder.encode(request.password()));
        return user;
    }

    public void delete(Long id) {
        User user = getById(id);
        if (user.getId().equals(getCurrentUser().getId()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Нельзя удалить самого себя");
        userRepository.delete(user);
    }

    private User createUser(UserRequest request, Role role) {
        if (request.username() == null || !request.username().matches("[a-zA-Z0-9_]{3,30}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Логин: от 3 до 30 латинских букв, цифр или _");
        checkPassword(request.password());
        if (request.name() == null || request.name().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не указано имя");
        if (userRepository.existsByUsername(request.username()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Такой логин уже занят");
        if (request.email() != null && userRepository.existsByEmail(request.email()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Пользователь с таким email уже есть");

        User user = new User();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setName(request.name());
        user.setEmail(request.email());
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private void checkPassword(String password) {
        if (password == null || password.length() < 8 || !password.matches(".*[a-zA-Z].*") || !password.matches(".*\\d.*"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Пароль должен быть не короче 8 символов и содержать буквы и цифры");
    }
}
