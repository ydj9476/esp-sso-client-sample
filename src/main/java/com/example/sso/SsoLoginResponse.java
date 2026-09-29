package com.example.sso;

/**
 * esp-api 토큰 검증 응답.
 *
 * <pre>
 * 성공(200): { "header": { "resCode": "success" },
 *              "data":   { "loginId": "admin" } }
 * 실패(4xx/5xx): { "header": { "resCode": "ESP092", "resMsg": "유효하지 않거나 만료된 SSO 토큰입니다." } }
 * </pre>
 */
public class SsoLoginResponse {

    private Header header;
    private Data data;

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Header {

        private String resCode; // 성공: "success", 실패: ESP 오류 코드 (예: ESP092)
        private String resMsg; // 실패 메시지

        public String getResCode() {
            return resCode;
        }

        public void setResCode(String resCode) {
            this.resCode = resCode;
        }

        public String getResMsg() {
            return resMsg;
        }

        public void setResMsg(String resMsg) {
            this.resMsg = resMsg;
        }
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
