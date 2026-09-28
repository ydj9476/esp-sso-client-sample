package com.example.sso.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.example.sso.config.SsoProperties;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;

class EspJwtVerifierTest {

    private static final KeyPair ESP_KEYS = Jwts.SIG.RS256.keyPair().build();
    private static final KeyPair ATTACKER_KEYS = Jwts.SIG.RS256.keyPair().build();

    private final EspJwtVerifier verifier = new EspJwtVerifier(
            ESP_KEYS.getPublic(),
            new SsoProperties(null, "esp", "3rd-A", Duration.ofSeconds(30)));

    private static JwtBuilder validClaims() {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer("esp")
                .subject("admin")
                .audience().add("3rd-A").and()
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(60)));
    }

    @Test
    void 정상_토큰은_로그인ID를_반환한다() {
        String token = validClaims().signWith(ESP_KEYS.getPrivate(), Jwts.SIG.RS256).compact();

        VerifiedUser user = verifier.verify(token);

        assertThat(user.loginId()).isEqualTo("admin");
        assertThat(user.tokenId()).isNotBlank();
    }

    @Test
    void payload를_변조하면_거부한다() {
        String token = validClaims().signWith(ESP_KEYS.getPrivate(), Jwts.SIG.RS256).compact();
        String[] parts = token.split("\\.");
        Base64.Decoder dec = Base64.getUrlDecoder();
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        String payload = new String(dec.decode(parts[1]), StandardCharsets.UTF_8)
                .replace("\"admin\"", "\"superadmin\"");
        String tampered = parts[0] + "." + enc.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + parts[2];

        assertThatThrownBy(() -> verifier.verify(tampered)).isInstanceOf(SsoAuthException.class);
    }

    @Test
    void 다른_키로_서명하면_거부한다() {
        String token = validClaims().signWith(ATTACKER_KEYS.getPrivate(), Jwts.SIG.RS256).compact();

        assertThatThrownBy(() -> verifier.verify(token)).isInstanceOf(SsoAuthException.class);
    }

    @Test
    void 다른_시스템용_토큰은_거부한다() {
        String token = validClaims()
                .claim("aud", "3rd-B")
                .signWith(ESP_KEYS.getPrivate(), Jwts.SIG.RS256).compact();

        assertThatThrownBy(() -> verifier.verify(token)).isInstanceOf(SsoAuthException.class);
    }

    @Test
    void 다른_발급자의_토큰은_거부한다() {
        String token = validClaims()
                .issuer("evil")
                .signWith(ESP_KEYS.getPrivate(), Jwts.SIG.RS256).compact();

        assertThatThrownBy(() -> verifier.verify(token)).isInstanceOf(SsoAuthException.class);
    }

    @Test
    void 만료된_토큰은_거부한다() {
        Instant past = Instant.now().minusSeconds(120);
        String token = validClaims()
                .issuedAt(Date.from(past.minusSeconds(60)))
                .expiration(Date.from(past))
                .signWith(ESP_KEYS.getPrivate(), Jwts.SIG.RS256).compact();

        assertThatThrownBy(() -> verifier.verify(token)).isInstanceOf(SsoAuthException.class);
    }

    @Test
    void 서명없는_alg_none_토큰은_거부한다() {
        String unsigned = validClaims().compact();

        assertThat(unsigned).endsWith(".");
        assertThatThrownBy(() -> verifier.verify(unsigned)).isInstanceOf(SsoAuthException.class);
    }

    @Test
    void RS256_이외_알고리즘은_거부한다() {
        String token = validClaims().signWith(ESP_KEYS.getPrivate(), Jwts.SIG.RS384).compact();

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(SsoAuthException.class)
                .hasMessageContaining("RS384");
    }

    @Test
    void jti가_없으면_거부한다() {
        String token = validClaims().id(null).signWith(ESP_KEYS.getPrivate(), Jwts.SIG.RS256).compact();

        assertThatThrownBy(() -> verifier.verify(token))
                .isInstanceOf(SsoAuthException.class)
                .hasMessageContaining("jti");
    }

    @Test
    void 빈_토큰은_거부한다() {
        assertThatThrownBy(() -> verifier.verify("")).isInstanceOf(SsoAuthException.class);
        assertThatThrownBy(() -> verifier.verify(null)).isInstanceOf(SsoAuthException.class);
    }
}
