package com.example.sso;

/**
 * esp-api 토큰 검증 응답.
 *
 * <pre>
 * 성공(200): { "header": { "resCode": "success" },
 *              "data":   { "loginId": "admin" } }
 * 실패(4xx/5xx): { "header": { "resCode": "ESP092", "resMsg": "유효하지 않거나 만료된 SSO 토큰입니다." } }
 * </pre>
 *
 * 성공 여부는 HTTP 상태로 판단하므로 data.loginId 만 받는다. (header 등 나머지 필드는 무시)
 */
public class SsoLoginResponse {

    private Data data;

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {

        private String loginId;

        public String getLoginId() {
            return loginId;
        }

        public void setLoginId(String loginId) {
            this.loginId = loginId;
        }
    }
}
