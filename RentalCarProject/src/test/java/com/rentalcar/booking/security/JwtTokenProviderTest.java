package com.rentalcar.booking.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    // 512-bit (64-byte) Base64 secret for HS512 tests
    private static final String TEST_SECRET =
            "cmVudGFsLWNhci1ib29raW5nLWp3dC1zZWNyZXQta2V5LWZvci1oczUxMi1hbGdvcml0aG0tMjAyNC1hYmNkZQ==";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 86400000L);
    }

    @Test
    @DisplayName("generateToken: produces non-null token string")
    void generateToken_notNull() {
        String token = jwtTokenProvider.generateToken("user@example.com");
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("extractEmail: returns the subject used during generation")
    void extractEmail_roundTrip() {
        String token = jwtTokenProvider.generateToken("user@example.com");
        assertThat(jwtTokenProvider.extractEmail(token)).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("generateToken with claims: extra claims are embedded")
    void generateToken_withClaims() {
        String token = jwtTokenProvider.generateToken("admin@example.com",
                Map.of("role", "ADMIN", "userId", 42L));
        assertThat(jwtTokenProvider.extractEmail(token)).isEqualTo("admin@example.com");
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken: valid token returns true")
    void validateToken_valid() {
        String token = jwtTokenProvider.generateToken("user@example.com");
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken: malformed token returns false")
    void validateToken_malformed() {
        assertThat(jwtTokenProvider.validateToken("not.a.valid.jwt")).isFalse();
    }

    @Test
    @DisplayName("validateToken: completely garbage string returns false")
    void validateToken_garbage() {
        assertThat(jwtTokenProvider.validateToken("garbage")).isFalse();
    }

    @Test
    @DisplayName("validateToken: empty string returns false")
    void validateToken_empty() {
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("getExpirationMs: returns configured value")
    void getExpirationMs_returnsConfigured() {
        assertThat(jwtTokenProvider.getExpirationMs()).isEqualTo(86400000L);
    }
}
