package webserver;

import db.MemoryUserRepository;
import enums.HttpHeader;
import enums.HttpMethod;
import enums.UrlPath;
import enums.UserParam;
import http.util.HttpRequestUtils;
import http.util.IOUtils;
import model.User;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RequestHandler implements Runnable{
    Socket connection;
    private static final Logger log = Logger.getLogger(RequestHandler.class.getName());

    public RequestHandler(Socket connection) {
        this.connection = connection;
    }

    @Override
    public void run() {
        log.log(Level.INFO, "New Client Connect! Connected IP : " + connection.getInetAddress() + ", Port : " + connection.getPort());
        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()){
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            DataOutputStream dos = new DataOutputStream(out);

            String line = br.readLine();
            System.out.println("요청 라인: " + line);

            if (line == null || line.isEmpty()) return;

            String[] tokens = line.split(" ");
            if (tokens.length < 2) return;

            // 요구사항 1 index.html을 반환하도록 함.
            String method = tokens[0];
            String urlPath = tokens[1];
            // tokens[2]에는 프로토콜이 들어있다.
            if (urlPath.equals(UrlPath.DEFAULT.path())) {
                urlPath = UrlPath.INDEX.path();
            }

            // 헤더 파싱!
            // body의 내용에 대한 length 가져오기. Content-Length 값.
            // 로그인되어있는지 쿠키 유무를 통해 확인한다.
            int contentLength = 0;
            // 쿠키가 있을 때만 true로 바꾼다.
            boolean isLoggedIn = false;
            String headerLine;
            while (br.ready() && !(headerLine = br.readLine()).isEmpty()) {
                // System.out.println(headerLine);
                // 요구사항 3번의 사용할 body를 가져오기 위해 필요함.
                if (headerLine.startsWith(HttpHeader.CONTENT_LENGTH.value())) {
                    contentLength = Integer.parseInt(headerLine.split(":")[1].trim());
                }
                // 요구사항 6에서 헤더의 쿠키에 Cookie: logined=true 가 있을 경우 isLoggedIn을 true로 바꿔준다.
                if (headerLine.startsWith(HttpHeader.COOKIE.value())) {
                    String cookie = headerLine.substring("Cookie:".length()).trim();
                    // 쿠키를 = 를 기준으로 나누어 저장한다. logined와 true가 나누어진다.
                    String[] loginCookie = cookie.split("=", 2);
                    if (loginCookie.length == 2 && loginCookie[0].trim().equals("logined")) {
                        // logined 뒤에 true일 때만 isLoggedIn이 true가 된다.
                        isLoggedIn = loginCookie[1].trim().equals("true");
                    }
                }

            }

            // 요구사항 2번. GET 방식으로 회원가입하기. form.html에서 form태그의 method가 get일 때
            if (method.equals(HttpMethod.GET.name()) && urlPath.startsWith(UrlPath.SIGNUP.path())) {
                // split을 통해 ?를 기준으로 경로와 쿼리를 나눈다.
                String[] pathSplit = urlPath.split("\\?", 2);
                if (pathSplit.length > 1) {
                    // 쿼리만 넘겨줄 것.
                    String queryString = pathSplit[1];
                    // parseQueryParameter를 통해 &를 기준으로 파라미터를 구분하고
                    // =를 기준으로 key와 value를 구분하여 Map에 저장한다.
                    Map<String, String> params = HttpRequestUtils.parseQueryParameter(queryString);
                    User user = new User(
                            params.get(UserParam.USER_ID.key()),
                            params.get(UserParam.PASSWORD.key()),
                            params.get(UserParam.NAME.key()),
                            params.get(UserParam.EMAIL.key())
                    );
                    MemoryUserRepository.getInstance().addUser(user);
                }
                response302Header(dos, UrlPath.INDEX.path());
                return;
            }

            // 요구사항 3번. form.html에서 form태그의 method가 post일 때
            if(method.equals(HttpMethod.POST.name())&&urlPath.startsWith(UrlPath.SIGNUP.path())) {
                // IOUtils.readData를 통해 요청에서 body 부분만 가져온다.
                String body = IOUtils.readData(br, contentLength);
                System.out.println("바디 : " + body);

                if (!body.isEmpty()) {
                    String queryString = body;
                    // parseQueryParameter를 통해 &를 기준으로 파라미터를 구분하고
                    // =를 기준으로 key와 value를 구분하여 Map에 저장한다.
                    Map<String, String> params = HttpRequestUtils.parseQueryParameter(queryString);
                    User user = new User(
                            params.get(UserParam.USER_ID.key()),
                            params.get(UserParam.PASSWORD.key()),
                            params.get(UserParam.NAME.key()),
                            params.get(UserParam.EMAIL.key())
                    );
                    System.out.println("생성된 유정 정보 : "+user);
                    MemoryUserRepository.getInstance().addUser(user);
                }
                response302Header(dos, UrlPath.INDEX.path());
                return;
            }

            // 요구사항 5번. 로그인 하기
            if (method.equals(HttpMethod.POST.name()) && urlPath.startsWith(UrlPath.LOGIN.path())) {
                String body = IOUtils.readData(br, contentLength);
                Map<String, String> params = HttpRequestUtils.parseQueryParameter(body);

                String userId = params.get(UserParam.USER_ID.key());
                String password = params.get(UserParam.PASSWORD.key());

                // id로 User 객체를 찾는다.
                User user = MemoryUserRepository.getInstance().findUserById(userId);

                // 해당 id의 User가 존재하고 입력 비밀번호가 해당 User의 비밀번호와 일치하면 쿠키를 추가하고 redirect한다.
                if (user != null && user.getPassword().equals(password)) {
                    response302HeaderWithCookie(dos, UrlPath.INDEX.path(), "logined=true");
                } else {
                    // 조건에 맞지 않으면 login_failed.html로 리다이렉트 시킨다.
                    response302Header(dos, UrlPath.LOGIN_FAILED.path());
                }
                return;
            }

            // 요구사항 6
            // 사용자 목록 출력 (요구사항 6)
            if (method.equals(HttpMethod.GET.name()) && urlPath.equals("/user/userList")) {
                // 쿠키에 logined=true가 없다면 login.html로 리다이렉트 시킨다.
                if (!isLoggedIn) {
                    response302Header(dos, UrlPath.LOGIN_PAGE.path());
                    return;
                }
                // 정적 파일 list.html 반환
                File file = new File("webapp" + UrlPath.USER_LIST.path());
                if (file.exists() && file.isFile()) {
                    byte[] body = Files.readAllBytes(file.toPath());
                    response200Header(dos, body.length, urlPath);
                    responseBody(dos, body);
                } else {
                    byte[] body = "404 Not Found".getBytes();
                    responseBody(dos, body);
                }
                return;
            }

            // 특정 경로에 대한 요청에 별도 처리가 없을 때 실행된다.
            // 위 요청들에 해당되지 않으면 실행된다.
            File file = new File("webapp" + urlPath);
            System.out.println("요청한 파일 경로: " + file.getAbsolutePath());

            if (file.exists() && file.isFile()) {
                byte[] body = Files.readAllBytes(file.toPath());
                response200Header(dos, body.length,urlPath);
                responseBody(dos, body);
            } else {
                byte[] body = "404 Not Found".getBytes();
                responseBody(dos, body);
            }
        } catch (IOException e) {
            log.log(Level.SEVERE,e.getMessage());
        }
    }

    // 200 : 요청이 성공적으로 처리됐을 때
    private void response200Header(DataOutputStream dos, int lengthOfBodyContent, String urlPath) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            String contentType = getContentType(urlPath);
            dos.writeBytes("Content-Type: "+contentType+";charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }

    private String getContentType(String urlPath) {
        if(urlPath.endsWith(".css")) return "text/css";
        return "text/html";
    }

    // 302 : 요청을 처리하고 클라이언트를 다른 url로 이동시키고 싶을 때 사용한다.
    // 브라우저는 Location: 을 보고 해당 url로 자동 이동시킨다.
    private void response302Header(DataOutputStream dos, String path) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found\r\n");
            dos.writeBytes("Location: " + path + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }
    // 로그인 시 쿠키를 헤더에 추가하여 index.html로 리다이렉트시킨다.
    private void response302HeaderWithCookie(DataOutputStream dos, String path, String cookie) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found\r\n");
            dos.writeBytes("Location: " + path + "\r\n");
            dos.writeBytes("Set-Cookie: " + cookie + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }


    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }

    // 404 추가하기.
}
