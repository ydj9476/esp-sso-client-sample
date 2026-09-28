package com.example.sso.config;

import java.security.PublicKey;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.sso.jwt.EspJwtVerifier;
import com.example.sso.jwt.PemKeys;

@Configuration
public class SsoConfig {

    @Bean
    public EspJwtVerifier espJwtVerifier(SsoProperties properties) {
        PublicKey publicKey = PemKeys.readPublicKey(properties.publicKeyLocation());
        return new EspJwtVerifier(publicKey, properties);
    }
}
