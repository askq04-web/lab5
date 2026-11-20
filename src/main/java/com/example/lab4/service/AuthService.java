package com.example.lab4.service;

import com.example.lab4.dto.*;
import com.example.lab4.model.*;
import com.example.lab4.repository.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;

@Service
@Transactional
public class AuthService {
    @Autowired private UserRepository users;
    @Autowired private UserSessionRepository sessions;
    @Autowired private UserService userService;
    @Autowired private PasswordEncoder encoder;
    @Autowired private JwtService jwt;

    public TokenPair login(String username, String password, String deviceId) {
        required(username);
        required(password);
        User user = activeUser(username);
        if (!encoder.matches(password, user.getPassword())) throw unauthorized();
        return issue(user, deviceId);
    }

    public TokenPair register(UserRequest request, String deviceId) {
        return issue(userService.register(request), deviceId);
    }

    public TokenPair refresh(String token) {
        required(token);
        Claims claims;
        try {
            claims = jwt.validateRefresh(token);
        } catch (JwtException | IllegalArgumentException e) {
            throw unauthorized();
        }
        UserSession old = sessions.findByRefreshToken(token).orElseThrow(this::unauthorized);
        User user = activeUser(claims.getSubject());
        if (!old.getUsername().equals(user.getUsername())
                || !old.getRefreshTokenExpiry().isAfter(Instant.now())) throw unauthorized();
        if (old.getStatus() == SessionStatus.USED) {
            old.setStatus(SessionStatus.REVOKED);
            return null; // Контроллер вернёт 401 после фиксации транзакции.
        }
        if (old.getStatus() != SessionStatus.ACTIVE) throw unauthorized();
        old.setStatus(SessionStatus.USED);
        return issue(user, old.getDeviceId());
    }

    private TokenPair issue(User user, String deviceId) {
        String access = jwt.createAccess(user.getUsername());
        String refresh = jwt.createRefresh(user.getUsername());
        sessions.saveAndFlush(new UserSession(user.getUsername(), deviceId, access, refresh,
                jwt.validateAccess(access).getExpiration().toInstant(),
                jwt.validateRefresh(refresh).getExpiration().toInstant()));
        return new TokenPair(access, refresh);
    }

    private User activeUser(String username) {
        User user = users.findByUsername(username).orElseThrow(this::unauthorized);
        if (user.getStatus() != UserStatus.ACTIVE) throw unauthorized();
        return user;
    }

    private void required(String value) {
        if (value == null || value.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не заполнено обязательное поле");
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверные учётные данные или refresh");
    }
}
