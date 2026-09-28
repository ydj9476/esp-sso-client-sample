package com.example.sso.web;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.sso.jwt.EspJwtVerifier;
import com.example.sso.jwt.SsoAuthException;
import com.example.sso.jwt.UsedTokenStore;
import com.example.sso.jwt.VerifiedUser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * esp에서 넘어오는 SSO 진입점.
 *
 * <pre>
 * POST /sso  (application/x-www-form-urlencoded, token=JWT)
 *   → 서명·클레임 검증 → jti 1회용 처리 → 세션 생성
 *   → 302 /main  (PRG 패턴: 새로고침 시 토큰 재전송 방지)
 * </pre>
 */
@RestController
public class SsoController {

    public static final String SESSION_LOGIN_ID = "loginId";

    private static final Logger log = LoggerFactory.getLogger(SsoController.class);

    private final EspJwtVerifier verifier;
    private final UsedTokenStore usedTokenStore;

    public SsoController(EspJwtVerifier verifier, UsedTokenStore usedTokenStore) {
        this.verifier = verifier;
        this.usedTokenStore = usedTokenStore;
    }

    @PostMapping(value = "/sso", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> sso(@RequestParam(name = "token", required = false) String token,
                                      HttpServletRequest request) {
        VerifiedUser user;
        try {
            user = verifier.verify(token);
        } catch (SsoAuthException e) {
            log.warn("SSO 토큰 거부: {}", e.getMessage());
            return unauthorized();
        }

        if (!usedTokenStore.markUsed(user.tokenId(), user.expiresAt())) {
            log.warn("SSO 토큰 재사용 거부: jti={}, loginId={}", user.tokenId(), user.loginId());
            return unauthorized();
        }

        // 세션 고정 공격 방지: 기존 세션을 버리고 새로 발급
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_LOGIN_ID, user.loginId());

        // TODO(3rd 구현): loginId 로 자체 사용자 조회·권한 로딩

        log.info("SSO 로그인 성공: loginId={}", user.loginId());
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/main")).build();
    }

    private static ResponseEntity<String> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.TEXT_HTML)
                .body(Pages.message("로그인 실패", "유효하지 않거나 만료된 접근입니다. esp에서 다시 시도해 주세요."));
    }
}
