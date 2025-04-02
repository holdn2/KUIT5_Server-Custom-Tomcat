package http;

import enums.HttpHeader;
import enums.HttpMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class HttpRequestTest {
    @Test
    @DisplayName("request가 잘 파싱 되는지")
    void test_parse_get_request_correctly() throws Exception {
        String rawRequest =
                "GET /index.html HTTP/1.1\r\n" +
                        "Host: localhost:8080\r\n" +
                        "Connection: keep-alive\r\n" +
                        "\r\n";

        BufferedReader br = new BufferedReader(new StringReader(rawRequest));
        HttpRequest request = HttpRequest.from(br);

        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/index.html");
        assertThat(request.getHeader("Host")).isEqualTo("localhost:8080");
        assertThat(request.getBody()).isNull();
        assertThat(request.isMethod(HttpMethod.GET)).isTrue();
    }

    @Test
    @DisplayName("바디가 있을 때 잘 파싱되는지, post일 때")
    void test_parse_post_request_correctly() throws Exception {
        String rawRequest =
                "POST /user/login HTTP/1.1\r\n" +
                        "Host: localhost\r\n" +
                        "Content-Length: 25\r\n" +
                        "Content-Type: application/x-www-form-urlencoded\r\n" +
                        "\r\n" +
                        "userId=qwer&password=1234";

        BufferedReader br = new BufferedReader(new StringReader(rawRequest));
        HttpRequest request = HttpRequest.from(br);

        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/user/login");
        assertThat(request.getHeader(HttpHeader.CONTENT_LENGTH.value())).isEqualTo("25");
        assertThat(request.getBody()).isEqualTo("userId=qwer&password=1234");
        assertThat(request.isMethod(HttpMethod.POST)).isTrue();
    }

    @Test
    @DisplayName("쿠키를 헤더에 넣었을 때 userList를 볼 수 있도록 잘 되는지. 쿠키 검증 확인")
    void test_detect_login_cookie_true() throws Exception {
        String rawRequest =
                "GET /user/userList HTTP/1.1\r\n" +
                        "Cookie: logined=true\r\n" +
                        "\r\n";

        BufferedReader br = new BufferedReader(new StringReader(rawRequest));
        HttpRequest request = HttpRequest.from(br);

        assertThat(request.isLogined()).isTrue();
    }
    @Test
    @DisplayName("쿠키가 없을 때 isLogined를 false로 잘 만드는지")
    void test_detect_login_cookie_false_when_absent() throws Exception {
        String rawRequest =
                "GET /user/userList HTTP/1.1\r\n" +
                        "\r\n";

        BufferedReader br = new BufferedReader(new StringReader(rawRequest));
        HttpRequest request = HttpRequest.from(br);

        assertThat(request.isLogined()).isFalse();
    }
    @Test
    @DisplayName("쿠키에 이상한 값들이 들어가도 isLogined를 false로 만드는지")
    void test_detect_login_cookie_false_when_invalid() throws Exception {
        String rawRequest =
                "GET /user/userList HTTP/1.1\r\n" +
                        "Cookie: something=else\r\n" +
                        "\r\n";

        BufferedReader br = new BufferedReader(new StringReader(rawRequest));
        HttpRequest request = HttpRequest.from(br);

        assertThat(request.isLogined()).isFalse();
    }
}