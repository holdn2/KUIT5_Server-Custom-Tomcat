package webserver;

import db.MemoryUserRepository;
import enums.HttpHeader;
import enums.HttpMethod;
import enums.UrlPath;
import enums.UserParam;
import http.HttpRequest;
import http.HttpResponse;
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

            HttpRequest request = HttpRequest.from(br);
            HttpResponse response = new HttpResponse(out);


            // 요구사항 1 index.html을 반환하도록 함.
            String urlPath = request.getPath();
            // tokens[2]에는 프로토콜이 들어있다.
            if (urlPath.equals(UrlPath.DEFAULT.path())) {
                urlPath = UrlPath.INDEX.path();
            }

            // 요구사항 2번. GET 방식으로 회원가입하기. form.html에서 form태그의 method가 get일 때
            if (request.isMethod(HttpMethod.GET) && urlPath.startsWith(UrlPath.SIGNUP.path())) {
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
                response.redirect(UrlPath.INDEX.path());
                return;
            }

            // 요구사항 3번. form.html에서 form태그의 method가 post일 때
            if(request.isMethod(HttpMethod.POST)&&urlPath.startsWith(UrlPath.SIGNUP.path())) {
                // IOUtils.readData를 통해 요청에서 body 부분만 가져온다.
                String body = request.getBody();
                System.out.println("바디 : " + body);

                if (!body.isEmpty()) {
                    // parseQueryParameter를 통해 &를 기준으로 파라미터를 구분하고
                    // =를 기준으로 key와 value를 구분하여 Map에 저장한다.
                    Map<String, String> params = HttpRequestUtils.parseQueryParameter(body);
                    User user = new User(
                            params.get(UserParam.USER_ID.key()),
                            params.get(UserParam.PASSWORD.key()),
                            params.get(UserParam.NAME.key()),
                            params.get(UserParam.EMAIL.key())
                    );
                    // System.out.println("생성된 유정 정보 : "+user);
                    MemoryUserRepository.getInstance().addUser(user);
                }
                response.redirect(UrlPath.INDEX.path());
                return;
            }

            // 요구사항 5번. 로그인 하기
            if (request.isMethod(HttpMethod.POST) && urlPath.startsWith(UrlPath.LOGIN.path())) {
                String body = request.getBody();
                Map<String, String> params = HttpRequestUtils.parseQueryParameter(body);

                String userId = params.get(UserParam.USER_ID.key());
                String password = params.get(UserParam.PASSWORD.key());

                // id로 User 객체를 찾는다.
                User user = MemoryUserRepository.getInstance().findUserById(userId);

                // 해당 id의 User가 존재하고 입력 비밀번호가 해당 User의 비밀번호와 일치하면 쿠키를 추가하고 redirect한다.
                if (user != null && user.getPassword().equals(password)) {
                    response.redirectWithCookie(UrlPath.INDEX.path(), "logined=true");
                } else {
                    // 조건에 맞지 않으면 login_failed.html로 리다이렉트 시킨다.
                    response.redirect(UrlPath.LOGIN_FAILED.path());
                }
                return;
            }

            // 요구사항 6
            // 사용자 목록 출력 (요구사항 6)
            if (request.isMethod(HttpMethod.GET) && urlPath.equals("/user/userList")) {
                if (!request.isLogined()) {
                    response.redirect(UrlPath.LOGIN_PAGE.path());
                    return;
                }
                response.forward(UrlPath.USER_LIST.path());
                return;
            }

            response.forward(urlPath);
        } catch (IOException e) {
            log.log(Level.SEVERE,e.getMessage());
        }
    }
}
