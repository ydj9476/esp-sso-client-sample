package com.example.sso;

import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClient;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class SsoController {

    private static final Logger log = LoggerFactory.getLogger(SsoController.class);

    private final RestClient espApi;
    private final String verifyPath;

    public SsoController(@Value("${esp.base-url}") String baseUrl,
            @Value("${esp.verify-path}") String verifyPath,
            @Value("${esp.connect-timeout-sec:2}") int connectTimeoutSec,
            @Value("${esp.read-timeout-sec:3}") int readTimeoutSec,
            @Value("${esp.trust-all-cert:true}") boolean trustAllCert) {

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient(connectTimeoutSec, trustAllCert));
        factory.setReadTimeout(Duration.ofSeconds(readTimeoutSec));

        this.espApi = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.verifyPath = verifyPath;
    }

    /**
     * ESP 호출용 HTTP 클라이언트.
     *
     * @param trustAllCert true 면 SSL 인증서 검증을 생략한다
     */
    private static HttpClient httpClient(int connectTimeoutSec, boolean trustAllCert) {
        HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(connectTimeoutSec));
        if (trustAllCert) {
            builder.sslContext(trustAllSslContext());
        }

        return builder.build();
    }

    /**
     * 인증서 검증을 생략하는 SSLContext.
     * ESP 가 사설 CA(Avaya System Manager CA 등) 가 발급한 인증서를 쓰는 경우, JVM 기본 truststore 로는
     * 체인 검증이 불가능하다 (PKIX path building failed). 그 CA 인증서를 3rd 서버 truststore 에 등록하는 것이 정석이지만
     * 등록이 어려운 환경을 위해 검증을 생략할 수 있게 열어둔다.
     * ESP 가 공인 인증서를 쓰는 환경이라면 application.yml 의 esp.trust-all-cert 를 false 로 두는 것을
     * 권장한다.
     */
    private static SSLContext trustAllSslContext() {
        TrustManager trustAll = new X509TrustManager() {

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                // 검증하지 않는다
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                // 검증하지 않는다
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { trustAll }, new SecureRandom());

            return sslContext;
        } catch (Exception e) {
            throw new IllegalStateException("SSLContext 생성 실패", e);
        }
    }

    /**
     * esp 에서 넘어오는 SSO 진입점. esp 가 띄운 팝업이 GET /sso?token=xxx 로 이동해 온다.
     * 1. 받은 토큰을 esp-api 에 검증 요청
     * 2. 검증되면 3rd 자체 사용자 확인 후 세션에 loginId 저장
     * 3. 메인 화면으로 redirect (주소창·새로고침에 토큰이 남지 않도록)
     * 오류: ESP 연동 오류는 /esp-error, 3rd 자체 오류는 /3rd-error 로 redirect
     * /sso 는 처리만 하고 화면은 각 경로(/main, /esp-error, /3rd-error)가 보여준다.
     */
    @GetMapping("/sso")
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
                return "redirect:/3rd-error";
            }

            // 3. 세션에 로그인ID 저장 (서버 메모리, 브라우저에는 세션ID 쿠키만 전달)
            HttpSession session = request.getSession();
            session.setAttribute("loginId", loginId);

            log.info("SSO 로그인 성공: loginId[{}]", loginId);

            return "redirect:/main";
        } catch (Exception e) {
            // 응답 형식이 다른 경우 등
            log.error("SSO 로그인 실패 [ESP] 처리 중 오류", e);

            return "redirect:/esp-error";
        }
    }

    /** 로그인 후 메인 화면. 세션이 있으면 static/main-page.html 을 보여준다. */
    @GetMapping("/main")
    public String main(HttpSession session) {
        if (session.getAttribute("loginId") == null) {
            return "redirect:/3rd-error";
        }

        return "forward:/main-page.html";
    }

    /** ESP 연동 오류 화면 (static/esp-error.html). */
    @GetMapping("/esp-error")
    public String espError() {
        return "forward:/esp-error.html";
    }

    /** 3rd 시스템 오류 화면 (static/3rd-error.html). */
    @GetMapping("/3rd-error")
    public String thirdError() {
        return "forward:/3rd-error.html";
    }

}
