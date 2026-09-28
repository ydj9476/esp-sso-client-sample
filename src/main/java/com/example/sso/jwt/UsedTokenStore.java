package com.example.sso.jwt;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * 사용된 토큰(jti)을 기록해 재사용을 막는다.
 *
 * <p>토큰 만료 시각까지만 보관하면 충분하다 (만료 후에는 검증 단계에서 이미 거부됨).
 *
 * <p>샘플은 단일 서버 메모리 구현이다. 서버가 여러 대이면 Redis
 * ({@code SET jti 1 NX EX <남은초>}) 또는 DB unique 제약으로 교체해야 한다.
 */
@Component
public class UsedTokenStore {

    private final Map<String, Instant> used = new ConcurrentHashMap<>();
    private final Clock clock;

    public UsedTokenStore() {
        this(Clock.systemUTC());
    }

    UsedTokenStore(Clock clock) {
        this.clock = clock;
    }

    /**
     * @return 처음 사용이면 true, 이미 사용된 토큰이면 false
     */
    public boolean markUsed(String tokenId, Instant expiresAt) {
        Instant now = clock.instant();
        used.values().removeIf(exp -> exp.isBefore(now));
        return used.putIfAbsent(tokenId, expiresAt) == null;
    }
}
