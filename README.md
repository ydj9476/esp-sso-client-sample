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
POST {esp 주소}/esp/api/v1/auth/sso/loginid
Content-Type: application/json

{ "token": "b3f1c2e4-..." }
```

응답 — HTTP `200` 이면 성공, 그 외(4xx/5xx)는 실패:

```json
{ "header": { "resCode": "success" },
  "data":   { "loginId": "admin" } }
```

### ③ 로그인 처리

- 성공: 3rd 자체 사용자 확인 → 세션에 `loginId` 저장 → `/main` 으로 redirect (`302`, 새로고침 시 토큰 재전송 방지) → 세션 확인 후 `main.html` 표시
- 실패
  - ESP 연동 오류 (토큰 만료·재사용, esp-api 오류 응답, 타임아웃·연결 실패 등) → `esp-error.html`
  - 3rd 자체 오류 (3rd 에 없는 사용자 등, 3rd 가 구현) → `3rd-error.html`

## 파일

| 파일 | 내용 |
|---|---|
| `SsoController.java` | 위 ①②③ 전부 |
| `SsoLoginResponse.java` | esp-api 검증 응답 DTO |
| `static/main-page.html` | 메인 화면 (샘플, `/main` 에서 forward) |
| `static/esp-error.html` | ESP 연동 오류 화면 |
| `static/3rd-error.html` | 3rd 시스템 오류 화면 |
| `application.yml` | esp 주소, 검증 API 경로, 타임아웃, 인증서 옵션 |

## 실행

```bash
./gradlew bootRun
```

## 준비 사항

- 3rd 서버 → esp-api 통신 가능 (방화벽 오픈)
- ESP 가 **사설 CA 인증서**(Avaya System Manager CA 등)를 쓰는 경우, JVM 기본 truststore 로는
  체인 검증이 안 되어 호출이 아래 오류로 실패한다.

  ```
  PKIX path building failed: unable to find valid certification path to requested target
  ```

  CA 인증서를 3rd 서버 truststore 에 등록하는 것이 정석이지만, 등록이 어려우면
  `application.yml` 의 `esp.trust-all-cert` 를 `true` 로 두면 된다 (기본값).
  ESP 가 공인 인증서를 쓰는 환경이면 `false` 를 권장한다.
