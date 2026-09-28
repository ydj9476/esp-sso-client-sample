package com.example.sso.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class UsedTokenStoreTest {

    @Test
    void 같은_jti는_한번만_사용할_수_있다() {
        UsedTokenStore store = new UsedTokenStore();
        Instant exp = Instant.now().plusSeconds(60);

        assertThat(store.markUsed("jti-1", exp)).isTrue();
        assertThat(store.markUsed("jti-1", exp)).isFalse();
        assertThat(store.markUsed("jti-2", exp)).isTrue();
    }

    @Test
    void 만료된_기록은_정리된다() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        UsedTokenStore store = new UsedTokenStore(Clock.fixed(now, ZoneOffset.UTC));

        assertThat(store.markUsed("old", now.minusSeconds(1))).isTrue();
        // 다음 호출 시 만료된 "old" 는 정리되므로 다시 등록 가능 (검증 단계에서 exp 로 이미 거부되는 토큰)
        assertThat(store.markUsed("old", now.plusSeconds(60))).isTrue();
    }
}
