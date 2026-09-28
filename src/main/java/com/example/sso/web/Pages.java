package com.example.sso.web;

/** 샘플용 최소 HTML. 실제 3rd 화면으로 대체된다. */
final class Pages {

    private Pages() {
    }

    /** title 은 고정 문자열만, bodyHtml 은 호출부에서 escape 된 값만 전달할 것. */
    static String message(String title, String bodyHtml) {
        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head><meta charset="UTF-8"><title>%s</title></head>
                <body style="font-family:sans-serif;padding:40px">
                  <h2>%s</h2>
                  <p>%s</p>
                </body>
                </html>
                """.formatted(title, title, bodyHtml);
    }
}
