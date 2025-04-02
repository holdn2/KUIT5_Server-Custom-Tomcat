package http;

import enums.HttpHeader;
import enums.HttpMethod;
import http.util.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    private String method;
    private String path;
    private String version;
    private final Map<String, String> headers = new HashMap<>();
    private String body;

    private HttpRequest() {}

    public static HttpRequest from(BufferedReader br) throws IOException {
        HttpRequest request = new HttpRequest();

        // Start Line 처리
        String line = br.readLine();
        String[] startLine = line.split(" ");
        request.method = startLine[0];
        request.path = startLine[1];
        request.version = startLine[2];

        // Header 처리
        String headerLine;
        while ((headerLine = br.readLine()) != null && !headerLine.isEmpty()) {
            String[] parts = headerLine.split(": ", 2);
            if (parts.length == 2) {
                request.headers.put(parts[0], parts[1]);
            }
        }

        // Body 처리. Content-Length 활용
        String contentLengthHeader = request.headers.get(HttpHeader.CONTENT_LENGTH.value());
        if (contentLengthHeader != null) {
            int contentLength = Integer.parseInt(contentLengthHeader.trim());
            request.body = IOUtils.readData(br, contentLength);
        }

        return request;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getHeader(String key) {
        return headers.get(key);
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    // 쿠키 처리 메서드. 로그인이 되어있는지 확인.
    public boolean isLogined() {
        String cookie = getHeader(HttpHeader.COOKIE.value());
        if (cookie != null) {
            String[] loginCookie = cookie.split("=", 2);
            return loginCookie.length == 2 && loginCookie[0].trim().equals("logined")
                    && loginCookie[1].trim().equals("true");
        }
        return false;
    }

    // 메서드 확인을 위한 편의 함수
    public boolean isMethod(HttpMethod httpMethod) {
        return this.method.equalsIgnoreCase(httpMethod.name());
    }
}
