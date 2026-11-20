package com.example.lab4;

import com.example.lab4.dto.*;
import com.example.lab4.model.*;
import com.example.lab4.repository.*;
import com.example.lab4.service.*;
import com.fasterxml.jackson.databind.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=${LAB5_TEST_URL:jdbc:h2:mem:lab5;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1}",
        "spring.datasource.username=${LAB5_TEST_USER:sa}", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=${LAB5_TEST_DDL:create-drop}", "spring.jpa.show-sql=false",
        "jwt.secret=lab5-test-secret-at-least-32-characters-long",
        "jwt.access-seconds=900", "jwt.refresh-seconds=604800"
})
@AutoConfigureMockMvc
class AuthTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired AuthService auth;
    @Autowired JwtService jwt;
    @Autowired UserRepository users;
    @MockitoSpyBean UserSessionRepository sessions;

    @BeforeEach
    void clean() {
        reset(sessions);
        sessions.deleteAll();
        users.deleteAll();
    }

    TokenPair register() {
        return auth.register(new UserRequest("student", "Password123", "Студент", null,
                Role.ADMIN, UserStatus.BLOCKED), "laptop");
    }

    ResultActions postJson(String path, Object body) throws Exception {
        return mvc.perform(post(path).contentType("application/json").content(json.writeValueAsBytes(body)));
    }

    @Test
    void rotationAndReplay() throws Exception {
        register();
        assertThat(users.findByUsername("student").orElseThrow().getRole()).isEqualTo(Role.CUSTOMER);
        sessions.deleteAll();
        String response = postJson("/auth/login", Map.of("username", "student", "password", "Password123",
                        "deviceId", "phone")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        TokenPair first = json.readValue(response, TokenPair.class);
        mvc.perform(get("/api/samples").header("Authorization", "Bearer " + first.accessToken()))
                .andExpect(status().isOk());
        String refreshed = postJson("/auth/refresh", Map.of("refreshToken", first.refreshToken()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        TokenPair second = json.readValue(refreshed, TokenPair.class);
        assertThat(second.accessToken()).isNotEqualTo(first.accessToken());
        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThat(sessions.findAll()).hasSize(2);
        assertThat(session(first).getStatus()).isEqualTo(SessionStatus.USED);
        assertThat(session(second).getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(session(second).getDeviceId()).isEqualTo("phone");
        for (UserSession s : sessions.findAll()) {
            assertThat(s.getAccessTokenExpiry()).isEqualTo(jwt.validateAccess(s.getAccessToken()).getExpiration().toInstant());
            assertThat(s.getRefreshTokenExpiry()).isEqualTo(jwt.validateRefresh(s.getRefreshToken()).getExpiration().toInstant());
        }
        postJson("/auth/refresh", Map.of("refreshToken", first.refreshToken()))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.accessToken").doesNotExist());
        assertThat(session(first).getStatus()).isEqualTo(SessionStatus.REVOKED);
        assertThat(session(second).getStatus()).isEqualTo(SessionStatus.ACTIVE);
        postJson("/auth/refresh", Map.of("refreshToken", first.refreshToken())).andExpect(status().isUnauthorized());
        assertThat(sessions.count()).isEqualTo(2);
    }

    UserSession session(TokenPair pair) {
        return sessions.findAll().stream().filter(s -> s.getRefreshToken().equals(pair.refreshToken())).findFirst().orElseThrow();
    }

    @Test
    void httpValidation() throws Exception {
        postJson("/auth/register", Map.of("username", "student", "password", "Password123", "name", "Студент",
                "role", "ADMIN")).andExpect(status().isCreated()).andExpect(jsonPath("$.refreshToken").isString());
        postJson("/auth/register", Map.of("username", "student", "password", "Password123", "name", "Студент"))
                .andExpect(status().isConflict());
        postJson("/auth/register", Map.of("username", "other", "password", "123", "name", "Студент"))
                .andExpect(status().isBadRequest());
        postJson("/auth/register", Map.of("username", "other", "password", "Password123"))
                .andExpect(status().isBadRequest());
        postJson("/auth/login", Map.of("username", "student", "password", "wrong")).andExpect(status().isUnauthorized());
        postJson("/auth/login", Map.of("username", "missing", "password", "Password123")).andExpect(status().isUnauthorized());
        postJson("/auth/login", Map.of("username", "student")).andExpect(status().isBadRequest());
        postJson("/auth/login", Map.of("username", " ", "password", "x")).andExpect(status().isBadRequest());
        postJson("/auth/refresh", Map.of()).andExpect(status().isBadRequest());
        postJson("/auth/refresh", Map.of("refreshToken", " ")).andExpect(status().isBadRequest());
        postJson("/auth/refresh", Map.of("refreshToken", "wrong")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/samples")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongTokenTypesAndClaims() throws Exception {
        TokenPair pair = register();
        postJson("/auth/refresh", Map.of("refreshToken", pair.accessToken())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/samples").header("Authorization", "Bearer " + pair.refreshToken()))
                .andExpect(status().isUnauthorized());
        postJson("/auth/refresh", Map.of("refreshToken", jwt.createRefresh("student"))).andExpect(status().isUnauthorized());
        var key = Keys.hmacShaKeyFor("lab5-test-secret-at-least-32-characters-long".getBytes(StandardCharsets.UTF_8));
        for (String missing : List.of("sub", "jti", "iat", "exp", "type", "expired")) {
            var b = Jwts.builder();
            if (!missing.equals("sub")) b.subject("student");
            if (!missing.equals("jti")) b.id(UUID.randomUUID().toString());
            if (!missing.equals("iat")) b.issuedAt(new Date());
            if (!missing.equals("exp")) b.expiration(Date.from(Instant.now().plusSeconds(missing.equals("expired") ? -10 : 60)));
            if (!missing.equals("type")) b.claim("type", "refresh");
            String token = b.signWith(key, Jwts.SIG.HS256).compact();
            postJson("/auth/refresh", Map.of("refreshToken", token)).andExpect(status().isUnauthorized());
        }
        String forged = Jwts.builder().subject("student").id("x").issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(60))).claim("type", "refresh")
                .signWith(Jwts.SIG.HS256.key().build()).compact();
        postJson("/auth/refresh", Map.of("refreshToken", forged)).andExpect(status().isUnauthorized());
        assertThat(sessions.count()).isEqualTo(1);
    }

    @Test
    void blockedAndDeletedUsers() throws Exception {
        TokenPair pair = register();
        User user = users.findByUsername("student").orElseThrow();
        user.setStatus(UserStatus.BLOCKED);
        users.save(user);
        postJson("/auth/login", Map.of("username", "student", "password", "Password123")).andExpect(status().isUnauthorized());
        postJson("/auth/refresh", Map.of("refreshToken", pair.refreshToken())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/samples").header("Authorization", "Bearer " + pair.accessToken())).andExpect(status().isUnauthorized());
        users.delete(user);
        postJson("/auth/refresh", Map.of("refreshToken", pair.refreshToken())).andExpect(status().isUnauthorized());
    }

    @Test
    void storageFailureRollsBack() {
        TokenPair pair = register();
        doThrow(new IllegalStateException("Ошибка записи")).when(sessions).saveAndFlush(any(UserSession.class));
        assertThatThrownBy(() -> auth.refresh(pair.refreshToken())).isInstanceOf(IllegalStateException.class);
        assertThat(session(pair).getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(sessions.count()).isEqualTo(1);
        users.deleteAll();
        assertThatThrownBy(this::register).isInstanceOf(IllegalStateException.class);
        assertThat(users.count()).isZero();
    }

    @Test
    void concurrentRefresh() throws Exception {
        TokenPair pair = register();
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Callable<TokenPair> task = () -> { start.await(); return auth.refresh(pair.refreshToken()); };
            Future<TokenPair> a = pool.submit(task);
            Future<TokenPair> b = pool.submit(task);
            start.countDown();
            assertThat(Arrays.asList(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS)))
                    .filteredOn(Objects::nonNull).hasSize(1);
        }
        assertThat(sessions.count()).isEqualTo(2);
        assertThat(session(pair).getStatus()).isEqualTo(SessionStatus.REVOKED);
    }
}


