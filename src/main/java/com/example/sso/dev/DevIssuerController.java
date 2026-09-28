package com.example.sso.dev;

import java.security.PrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import com.example.sso.config.SsoProperties;
import com.example.sso.jwt.PemKeys;

import io.jsonwebtoken.Jwts;

/**
 * [local 프로파일 전용] esp 역할을 흉내 내는 테스트용 발급기.
 *
 * <p>esp-api 가 실제로 하는 일과 같다: 개인키로 JWT 를 서명하고,
 * 자동 submit form 으로 브라우저를 3rd 의 POST /sso 로 보낸다.
 *
 * <pre>
 * GET /dev/launch?id=admin          → 자동 POST /sso (브라우저 테스트)
 * GET /dev/token?id=admin&aud=3rd-B → JWT 문자열만 반환 (curl 테스트)
 * </pre>
 */
@Profile("local")
@RestController
public class DevIssuerController {

    private final PrivateKey privateKey;
    private final SsoProperties properties;
    private final Duration tokenTtl;

    public DevIssuerController(@Value("${esp.sso.dev.private-key-location}") Resource privateKeyLocation,
                               @Value("${esp.sso.dev.token-ttl:60s}") Duration tokenTtl,
                               SsoProperties properties) {
        this.privateKey = PemKeys.readPrivateKey(privateKeyLocation);
        this.tokenTtl = tokenTtl;
        this.properties = properties;
    }

    @GetMapping(value = "/dev/token", produces = MediaType.TEXT_PLAIN_VALUE)
    public String token(@RequestParam(defaultValue = "admin") String id,
                        @RequestParam(required = false) String aud) {
        return issue(id, aud != null ? aud : properties.audience());
    }

    @GetMapping(value = "/dev/launch", produces = MediaType.TEXT_HTML_VALUE)
    public String launch(@RequestParam(defaultValue = "admin") String id) {
        String token = HtmlUtils.htmlEscape(issue(id, properties.audience()));
        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head><meta charset="UTF-8"><title>이동 중</title></head>
                <body>
                  <form id="f" method="POST" action="/sso">
                    <input type="hidden" name="token" value="%s">
                    <noscript><button type="submit">계속</button></noscript>
                  </form>
                  <script>document.getElementById('f').submit();</script>
                </body>
                </html>
                """.formatted(token);
    }

    private String issue(String loginId, String audience) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(loginId)
                .audience().add(audience).and()
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(tokenTtl)))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }
}
