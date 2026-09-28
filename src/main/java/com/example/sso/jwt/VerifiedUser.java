package com.example.sso.jwt;

import java.time.Instant;

/**
 * 검증을 통과한 토큰 정보.
 *
 * @param loginId   esp 로그인ID (sub)
 * @param tokenId   토큰 고유 ID (jti) - 1회용 처리에 사용
 * @param expiresAt 토큰 만료 시각 (exp)
 */
public record VerifiedUser(String loginId, String tokenId, Instant expiresAt) {
}
