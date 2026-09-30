# esp-sso-client-sample

esp 링크 연동 시 **3rd 솔루션이 구현할 부분**의 Spring Boot 샘플입니다.

## 동작

1. 사용자가 esp 에서 링크를 클릭하면, esp 가 띄운 팝업이 3rd 의 `GET /sso?token=...` 으로 이동합니다 (**일회용 토큰**).
2. 3rd 서버는 그 토큰을 **esp-api 에 보내 검증**하고, 로그인ID 를 받습니다.
3. 3rd 는 로그인ID 로 세션을 만들고 메인 화면으로 이동합니다.

토큰의 만료(60초)·1회 사용 확인은 esp-api 가 합니다. 3rd 는 물어보기만 하면 됩니다.

## 3rd 가 할 일

### ① 토큰 받기

```
GET /sso?token=b3f1c2e4-...
```

esp 는 여기까지만 합니다. 이후 검증·세션·화면(오류 화면 포함)은 모두 3rd 가 처리합니다.

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

- 성공: 3rd 자체 사용자 확인 → 세션에 `loginId` 저장 → `/main` 으로 redirect (`302`, 주소창·새로고침에 토큰이 남지 않도록) → 세션 확인 후 `main-page.html` 표시
- 실패
  - ESP 연동 오류 (토큰 만료·재사용, esp-api 오류 응답, 타임아웃·연결 실패 등) → `/esp-error` 로 redirect → `esp-error.html` 표시
  - 3rd 자체 오류 (3rd 에 없는 사용자 등, 3rd 가 구현) → `/3rd-error` 로 redirect → `3rd-error.html` 표시
- `/sso` 는 처리만 하고 html 파일을 직접 가리키지 않습니다. 화면은 각 경로(`/main`, `/esp-error`, `/3rd-error`)가 보여줍니다.

## 프론트/API 가 분리된 구성

3rd 의 화면(html)과 API 서버가 분리되어 있어도 흐름은 같고, redirect 목적지만 다릅니다.

```
팝업 ─GET /api/sso?token=xxx─▶ 3rd API ─검증─▶ esp-api
     ◀─302 {프론트}/main.html + 세션쿠키─ 3rd API
팝업 ─GET /main.html─▶ 3rd 프론트 ─fetch /api/me (쿠키)─▶ 3rd API → loginId
```

- esp 메뉴 URL 에는 **3rd API 주소**(`/api/sso`)를 등록합니다.
- forward 는 같은 서버 안에서만 되므로, 다른 서버의 프론트로는 redirect 를 씁니다.
- **프론트와 API 는 같은 도메인으로 서비스**해야 세션쿠키가 공유됩니다 (예: `3rd.com/main.html`, `3rd.com/api/...`).
  서브도메인(`api.3rd.com`)이면 fetch 에 `credentials: 'include'`, API 에 CORS `allowCredentials(true)` 가 필요합니다.

API:

```java
@GetMapping("/api/sso")
public String sso(@RequestParam(name = "token", required = false) String token, HttpServletRequest request) {
    try {
        SsoLoginResponse res = espApi.post().uri(verifyPath)
                .body(Map.of("token", token)).retrieve().body(SsoLoginResponse.class);
        request.getSession().setAttribute("loginId", res.getData().getLoginId());

        return "redirect:" + frontUrl + "/main.html";
    } catch (Exception e) {
        return "redirect:" + frontUrl + "/error.html?type=esp";
    }
}

@GetMapping("/api/me")
@ResponseBody
public ResponseEntity<Map<String, String>> me(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    Object loginId = session == null ? null : session.getAttribute("loginId");
    if (loginId == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    return ResponseEntity.ok(Map.of("loginId", loginId.toString()));
}
```

프론트 `main.html`:

```html
<h2 id="welcome">로딩 중...</h2>
<script>
  fetch('/api/me', { credentials: 'include' })
    .then(function (res) {
      if (res.status === 401) {
        location.replace('error.html?type=session');
        return;
      }
      return res.json();
    })
    .then(function (data) {
      if (data) {
        document.getElementById('welcome').textContent = data.loginId + ' 님 환영합니다';
      }
    });
</script>
```

프론트 `error.html`:

```html
<p id="msg"></p>
<button onclick="window.close()">닫기</button>
<script>
  var MESSAGES = {
    esp: 'ESP 인증에 실패했습니다. ESP에서 다시 시도해 주세요.',
    session: '세션이 만료되었습니다. ESP에서 다시 접속해 주세요.'
  };
  var type = new URLSearchParams(location.search).get('type');
  document.getElementById('msg').textContent = MESSAGES[type] || '오류가 발생했습니다.';
</script>
```

## 파일

| 파일 | 내용 |
|---|---|
| `SsoController.java` | 위 ①②③ 전부 |
| `SsoLoginResponse.java` | esp-api 검증 응답 DTO |
| `static/main-page.html` | 메인 화면 (샘플, `/main` 에서 forward) |
| `static/esp-error.html` | ESP 연동 오류 화면 (`/esp-error` 에서 forward) |
| `static/3rd-error.html` | 3rd 시스템 오류 화면 (`/3rd-error` 에서 forward) |
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
- 토큰이 URL 로 전달되므로 3rd 웹서버 access log 에서 `token` 쿼리스트링 마스킹을 권장한다
  (토큰은 1회용·60초 만료라 재사용은 불가).
