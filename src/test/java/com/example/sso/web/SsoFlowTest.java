package com.example.sso.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.PrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.sso.jwt.PemKeys;

import io.jsonwebtoken.Jwts;

/** POST /sso → 302 /main → 로그인 사용자 표시 까지의 전체 흐름. */
@SpringBootTest
@AutoConfigureMockMvc
class SsoFlowTest {

    private static final PrivateKey DEV_PRIVATE_KEY =
            PemKeys.readPrivateKey(new ClassPathResource("keys/dev-private.pem"));

    @Autowired
    private MockMvc mvc;

    private static String issue(String loginId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer("esp")
                .subject(loginId)
                .audience().add("3rd-A").and()
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(60)))
                .signWith(DEV_PRIVATE_KEY, Jwts.SIG.RS256)
                .compact();
    }

    @Test
    void 토큰_검증_후_세션을_만들고_main으로_리다이렉트한다() throws Exception {
        MvcResult result = mvc.perform(post("/sso")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("token", issue("admin")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/main"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();

        mvc.perform(get("/main").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("admin")));
    }

    @Test
    void 같은_토큰을_다시_보내면_거부한다() throws Exception {
        String token = issue("admin");

        mvc.perform(post("/sso").contentType(MediaType.APPLICATION_FORM_URLENCODED).param("token", token))
                .andExpect(status().isFound());
        mvc.perform(post("/sso").contentType(MediaType.APPLICATION_FORM_URLENCODED).param("token", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 토큰이_없으면_거부한다() throws Exception {
        mvc.perform(post("/sso").contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 세션_없이_main에_접근하면_거부한다() throws Exception {
        mvc.perform(get("/main")).andExpect(status().isUnauthorized());
    }
}
