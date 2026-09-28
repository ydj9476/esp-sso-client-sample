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

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class SsoController {

    private static final Logger log = LoggerFactory.getLogger(SsoController.class);

    private final RestClient espApi;
    private final String verifyPath;
    private final String clientId;
    private final String apiKey;

    public SsoController(RestClient.Builder builder,
            @Value("${esp.base-url}") String baseUrl,
            @Value("${esp.verify-path}") String verifyPath,
            @Value("${esp.client-id}") String clientId,
            @Value("${ESP_SSO_API_KEY}") String apiKey) {
        this.espApi = builder.baseUrl(baseUrl).build();
        this.verifyPath = verifyPath;
        this.clientId = clientId;
        this.apiKey = apiKey;
    }

    /**
     * esp 에서 넘어오는 SSO 진입점.
     * 1. 받은 토큰을 esp-api 에 검증 요청
     * 2. 검증되면 세션에 loginId 저장
     * 3. 메인 화면으로 redirect (새로고침 시 토큰이 다시 전송되지 않도록)
     */
    @PostMapping("/sso")
    public String sso(@RequestParam(required = false) String token, HttpServletRequest request, HttpServletResponse response) {
        String loginId = verifyToken(token);
        if (loginId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return "login-fail";
        }

        HttpSession session = request.getSession();
        request.changeSessionId(); // 세션 고정 공격 방지
        session.setAttribute("loginId", loginId);

        // TODO: loginId 로 3rd 자체 사용자·권한 조회

        log.info("SSO 로그인 성공: loginId={}", loginId);

        return "redirect:/main";
    }

    /** 로그인 후 메인 화면 (샘플). */
    @GetMapping("/main")
    public String main(HttpSession session, Model model, HttpServletResponse response) {
        Object loginId = session.getAttribute("loginId");
        if (loginId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return "login-fail";
        }
        model.addAttribute("loginId", loginId);

        return "main";
    }

    /**
     * esp-api 에 토큰 검증 요청 (서버 간 통신).
     *
     * <pre>
     * 요청: POST /esp/api/v1/auth/sso
     *       X-SSO-CLIENT-ID: 3rd-A
     *       X-SSO-API-KEY:   ********
     *       { "token": "b3f1c2e4-..." }
     *
     * 응답: { "header": { "resCode": "success" },
     *         "data":   { "loginId": "admin" } }
     * </pre>
     *
     * @return 검증 성공 시 loginId, 실패 시 null
     */
    private String verifyToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            JsonNode res = espApi.post()
                    .uri(verifyPath)
                    .header("X-SSO-CLIENT-ID", clientId)
                    .header("X-SSO-API-KEY", apiKey)
                    .body(Map.of("token", token))
                    .retrieve()
                    .body(JsonNode.class);

            if (res != null && "success".equals(res.path("header").path("resCode").asText())) {
                String loginId = res.path("data").path("loginId").asText("");
                return loginId.isBlank() ? null : loginId;
            }
            log.warn("SSO 토큰 검증 실패: {}", res);
        } catch (Exception e) {
            log.warn("SSO 토큰 검증 실패: {}", e.getMessage());
        }

        return null;
    }
}
