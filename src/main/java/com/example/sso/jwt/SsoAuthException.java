package com.example.sso.jwt;

/** SSO 토큰 검증 실패. 원인 상세는 로그로만 남기고 사용자에게는 노출하지 않는다. */
public class SsoAuthException extends RuntimeException {

    public SsoAuthException(String message) {
        super(message);
    }

    public SsoAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
