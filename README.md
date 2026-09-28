# esp-jwt-sso-client-sample

esp 링크 연동(JWT 방식)에서 **3rd 솔루션이 구현해야 하는 수신·검증 부분**의 Spring Boot 샘플입니다.

## 흐름

```
esp-브라우저           esp-api                         3rd 솔루션 (이 샘플)
    │ 1. 링크 클릭        │                                  │
    │ 2. 토큰 발급 요청 ─▶ │ JWT 서명 (esp 개인키)              │
    │ ◀─ 3. JWT 응답      │                                  │
    │ 4. form POST /sso (token=JWT) ───────────────────────▶ │ 서명 검증 (esp 공개키)
    │                                                        │ iss·aud·exp·jti 확인 → 세션 생성
    │ ◀──────────────────────────── 5. 302 Location: /main ─ │
    │ 6. GET /main (세션 쿠키) ─────────────────────────────▶ │ 3rd 화면
```

3rd 가 esp-api 를 호출하는 서버 간 통신은 없습니다. 3rd 는 **사전에 전달받은 esp 공개키**만 있으면 됩니다.

## 3rd 연동 규격

| 항목 | 값 |
|---|---|
| 진입 URL | `POST /sso` (`application/x-www-form-urlencoded`, 파라미터 `token`) |
| 서명 알고리즘 | `RS256` 고정 (그 외, `alg=none` 거부) |
| `iss` | `esp` (합의값) |
| `aud` | 3rd 시스템 식별자, 예: `3rd-A` (합의값) |
| `sub` | esp 로그인ID |
| `exp` | 발급 후 60초 |
| `jti` | 토큰 고유 ID — **1회만 사용 허용** |
| 성공 응답 | 세션 생성 후 `302` 리다이렉트 (PRG, 새로고침 시 토큰 재전송 방지) |
| 실패 응답 | `401` (상세 사유는 로그에만) |

## 구성

| 파일 | 역할 |
|---|---|
| `jwt/EspJwtVerifier.java` | **핵심.** 서명·alg·iss·aud·exp·sub·jti 검증 |
| `jwt/UsedTokenStore.java` | jti 재사용 방지 (샘플은 메모리. 다중 서버면 Redis `SET NX EX` / DB unique 로 교체) |
| `jwt/PemKeys.java` | PEM 공개키/개인키 로드 |
| `web/SsoController.java` | `POST /sso` → 검증 → 세션 재발급 → `302 /main` |
| `web/MainController.java` | 로그인 후 화면 (샘플) |
| `dev/DevIssuerController.java` | **local 프로파일 전용** esp 역할 흉내 (테스트용 JWT 발급, 자동 submit) |

## 실행

Java 25, Spring Boot 3.5.3, jjwt 0.12.6

```bash
./gradlew test
./gradlew bootRun --args='--spring.profiles.active=local'
```

- 브라우저: `http://localhost:8080/dev/launch?id=admin` → 자동으로 `/sso` POST → `/main` 에 `admin` 표시
- curl:
  ```bash
  T=$(curl -s "http://localhost:8080/dev/token?id=admin")
  curl -i -X POST -d "token=$T" http://localhost:8080/sso            # 302 Location: /main
  curl -s -o /dev/null -w "%{http_code}" -X POST -d "token=$T" http://localhost:8080/sso   # 401 (재사용)
  ```

## 키

`src/main/resources/keys/` 의 키쌍은 **로컬 테스트 전용**입니다.

- `esp-public.pem` — 운영에서는 esp 로부터 전달받은 공개키로 교체
- `dev-private.pem` — `DevIssuerController` 전용. **운영 배포물에서 반드시 제거**

키쌍 생성 (esp 측 참고):

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out esp-private.pem
openssl pkey -in esp-private.pem -pubout -out esp-public.pem
```

## 운영 반영 시 체크리스트

- [ ] `esp-public.pem` 을 실제 esp 공개키로 교체, `dev-private.pem` 삭제
- [ ] `issuer`, `audience` 를 esp 와 합의한 값으로 설정
- [ ] 다중 서버면 `UsedTokenStore` 를 Redis/DB 로 교체
- [ ] HTTPS + `server.servlet.session.cookie.secure: true`
- [ ] `SsoController` 의 TODO: `loginId` 로 자체 사용자·권한 조회
