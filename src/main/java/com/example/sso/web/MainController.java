package com.example.sso.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import jakarta.servlet.http.HttpSession;

/** SSO 로그인 후 진입하는 3rd 메인 화면 (샘플). */
@RestController
public class MainController {

    @GetMapping(value = "/main", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> main(HttpSession session) {
        Object loginId = session.getAttribute(SsoController.SESSION_LOGIN_ID);
        if (loginId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Pages.message("로그인 필요", "esp에서 링크를 통해 접속해 주세요."));
        }
        String escaped = HtmlUtils.htmlEscape(loginId.toString());
        return ResponseEntity.ok(Pages.message("3rd 메인", "로그인 사용자: <b>" + escaped + "</b>"));
    }
}
