package com.example.lab4.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_sessions")
public class UserSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private String username;
    @Column(name = "device_id")
    private String deviceId;
    @Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
    private String accessToken;
    @Column(name = "refresh_token", nullable = false, unique = true, columnDefinition = "TEXT")
    private String refreshToken;
    @Column(name = "access_token_expiry", nullable = false)
    private Instant accessTokenExpiry;
    @Column(name = "refresh_token_expiry", nullable = false)
    private Instant refreshTokenExpiry;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    protected UserSession() {}

    public UserSession(String username, String deviceId, String accessToken, String refreshToken,
                       Instant accessTokenExpiry, Instant refreshTokenExpiry) {
        this.username = username;
        this.deviceId = deviceId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessTokenExpiry = accessTokenExpiry;
        this.refreshTokenExpiry = refreshTokenExpiry;
        this.status = SessionStatus.ACTIVE;
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getDeviceId() { return deviceId; }
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public Instant getAccessTokenExpiry() { return accessTokenExpiry; }
    public Instant getRefreshTokenExpiry() { return refreshTokenExpiry; }
    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }
}
