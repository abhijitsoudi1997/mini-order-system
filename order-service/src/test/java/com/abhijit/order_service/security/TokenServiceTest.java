package com.abhijit.order_service.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class TokenServiceTest {

    private static final String SECRET = "unit-test-secret-which-is-long-enough-32+";

    private final JwtConfig config = new JwtConfig();
    private final SecretKey key = config.jwtKey(SECRET);
    private final TokenService tokens = new TokenService(config.jwtEncoder(key), 30);

    private Authentication customer() {
        return new UsernamePasswordAuthenticationToken("customer", null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void tokenCarriesSubjectAndRolesWithoutThePrefix() {
        Jwt jwt = config.jwtDecoder(key).decode(tokens.issue(customer()));

        assertThat(jwt.getSubject()).isEqualTo("customer");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("USER");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("order-service");
    }

    @Test
    void tokenExpiresAfterTheConfiguredTime() {
        Jwt jwt = config.jwtDecoder(key).decode(tokens.issue(customer()));

        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(30));
        assertThat(tokens.expirySeconds()).isEqualTo(1800);
    }

    @Test
    void aSecretShorterThan32BytesIsRejected() {
        assertThatThrownBy(() -> config.jwtKey("too-short"))
                .isInstanceOf(IllegalStateException.class);
    }
}