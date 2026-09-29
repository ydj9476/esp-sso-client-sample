package com.example.sso;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClient;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class SsoController {

    private static final Logger log = LoggerFactory.getLogger(SsoController.class);

    private final RestClient espApi;
    private final String verifyPath;

    public SsoController(RestClient.Builder builder, @Value("${esp.base-url}") String baseUrl, @Value("${esp.verify-path}") String verifyPath) {
        this.espApi = builder.baseUrl(baseUrl).build();
        this.verifyPath = verifyPath;
    }

    /**
     * esp 에서 넘어오는 SSO 진입점.
     * 1. 받은 토큰을 esp-api 에 검증 요청
     * 2. 검증되면 3rd 자체 사용자 확인 후 세션에 loginId 저장
     * 3. 메인 화면으로 redirect (새로고침 시 토큰이 다시 전송되지 않도록)
     * 오류: ESP 연동 오류는 esp-error.html, 3rd 자체 오류는 3rd-error.html
     */
    @PostMapping("/sso")
    public String sso(@RequestParam(name = "token", required = false) String token, HttpServletRequest request) {
        try {
            // 1. 로그인ID 요청
            SsoLoginResponse res = espApi.post()
                    .uri(verifyPath)
                    .body(Map.of("token", token))
                    .retrieve()
                    .body(SsoLoginResponse.class);

            String loginId = res.getData().getLoginId();

            // 2. 3rd 자체 사용자 확인
            if (1 == 2) {
                log.warn("SSO 로그인 실패 [3RD] 3rd 미등록 사용자: loginId={}", loginId);
                return "redirect:/3rd-error.html";
            }

            // 3. 세션에 로그인ID 저장 (서버 메모리, 브라우저에는 세션ID 쿠키만 전달)
            HttpSession session = request.getSession();
            session.setAttribute("loginId", loginId);

            log.info("SSO 로그인 성공: loginId={}", loginId);

            return "redirect:/main.html";
        } catch (Exception e) {
            // 응답 형식이 다른 경우 등
            log.error("SSO 로그인 실패 [ESP] 처리 중 오류", e);
            return "redirect:/esp-error.html";
        }
    }

    /** 로그인 후 메인 화면 (샘플). */
    @GetMapping("/main")
    public String main(HttpSession session, Model model, HttpServletResponse response) {
        Object loginId = session.getAttribute("loginId");
        if (loginId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return "redirect:/esp-error.html";
        }

        model.addAttribute("loginId", loginId);

        return "main";
    }

}
