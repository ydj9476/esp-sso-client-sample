package com.example.sso.jwt;

import java.security.PublicKey;

import com.example.sso.config.SsoProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;

/**
 * esp가 발급한 SSO JWT를 esp 공개키로 직접 검증한다.
 *
 * <p>검증 항목
 * <ol>
 *   <li>서명: esp 공개키로 검증 (payload 변조 시 실패)</li>
 *   <li>알고리즘: RS256 고정 (alg=none, 다른 알고리즘 거부)</li>
 *   <li>iss: 허용한 발급자인지</li>
 *   <li>aud: 이 시스템용으로 발급된 토큰인지</li>
 *   <li>exp: 만료 여부 (필수 클레임)</li>
 *   <li>sub, jti: 필수 클레임 존재 여부</li>
 * </ol>
 */
public class EspJwtVerifier {

    private static final String REQUIRED_ALG = "RS256";

    private final JwtParser parser;

    public EspJwtVerifier(PublicKey espPublicKey, SsoProperties properties) {
        this.parser = Jwts.parser()
                .verifyWith(espPublicKey)
                .requireIssuer(properties.issuer())
                .requireAudience(properties.audience())
                .clockSkewSeconds(properties.clockSkew().toSeconds())
                .build();
    }

    public VerifiedUser verify(String token) {
        if (token == null || token.isBlank()) {
            throw new SsoAuthException("토큰 없음");
        }

        Jws<Claims> jws;
        try {
            // parseSignedClaims 는 서명 없는 토큰(alg=none)을 거부한다
            jws = parser.parseSignedClaims(token);
        } catch (JwtException | IllegalArgumentException e) {
            throw new SsoAuthException("토큰 검증 실패: " + e.getMessage(), e);
        }

        String alg = jws.getHeader().getAlgorithm();
        if (!REQUIRED_ALG.equals(alg)) {
            throw new SsoAuthException("허용하지 않는 알고리즘: " + alg);
        }

        Claims claims = jws.getPayload();
        if (claims.getExpiration() == null) {
            throw new SsoAuthException("exp 누락");
        }
        if (isBlank(claims.getSubject())) {
            throw new SsoAuthException("sub 누락");
        }
        if (isBlank(claims.getId())) {
            throw new SsoAuthException("jti 누락");
        }

        return new VerifiedUser(claims.getSubject(), claims.getId(), claims.getExpiration().toInstant());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
