# esp-sso-client-sample

esp 링크 연동 시 **3rd 솔루션이 구현할 부분**의 Spring Boot 샘플입니다.

## 동작

1. 사용자가 esp 에서 링크를 클릭하면, 브라우저가 3rd 의 `POST /sso` 로 **일회용 토큰**을 보냅니다.
2. 3rd 서버는 그 토큰을 **esp-api 에 보내 검증**하고, 로그인ID 를 받습니다.
3. 3rd 는 로그인ID 로 세션을 만들고 메인 화면으로 이동합니다.

토큰의 만료(60초)·1회 사용 확인은 esp-api 가 합니다. 3rd 는 물어보기만 하면 됩니다.

## 3rd 가 할 일

### ① 토큰 받기

```
POST /sso
Content-Type: application/x-www-form-urlencoded

token=b3f1c2e4-...
```

### ② esp-api 에 검증 요청

```
POST {esp 주소}/esp/api/v1/auth/sso
X-SSO-CLIENT-ID: 3rd-A          (esp 에서 발급)
X-SSO-API-KEY:   ********       (esp 에서 발급)
Content-Type:    application/json

{ "token": "b3f1c2e4-..." }
```

응답 — `header.resCode` 가 `success` 이면 성공:

```json
{ "header": { "resCode": "success" },
  "data":   { "loginId": "admin" } }
```

### ③ 로그인 처리

- 성공: 세션에 `loginId` 저장 → 메인 화면으로 redirect (`302`, 새로고침 시 토큰 재전송 방지)
- 실패: 로그인 실패 화면

## 파일

| 파일 | 내용 |
|---|---|
| `SsoController.java` | 위 ①②③ 전부 |
| `templates/main.html` | 메인 화면 (샘플) |
| `templates/login-fail.html` | 로그인 실패 화면 |
| `application.yml` | esp 주소, 검증 API 경로, client-id |

## 실행

```bash
ESP_SSO_API_KEY=발급받은값 ./gradlew bootRun
```

API Key 는 설정 파일에 넣지 않고 환경변수 `ESP_SSO_API_KEY` 로 넣습니다.

## 준비 사항

- esp 로부터 `client-id`, API Key 발급
- 3rd 서버 → esp-api 통신 가능 (방화벽 오픈)
