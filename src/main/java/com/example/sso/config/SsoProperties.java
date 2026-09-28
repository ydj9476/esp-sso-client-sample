package com.example.sso.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * esp SSO 연동 설정.
 *
 * @param publicKeyLocation esp 공개키(PEM) 위치
 * @param issuer            허용할 토큰 발급자(iss)
 * @param audience          이 시스템의 식별자(aud) - 다른 시스템용으로 발급된 토큰 거부
 * @param clockSkew         서버 간 시계 오차 허용치
 */
@ConfigurationProperties(prefix = "esp.sso")
public record SsoProperties(
        Resource publicKeyLocation,
        String issuer,
        String audience,
        Duration clockSkew) {
}
