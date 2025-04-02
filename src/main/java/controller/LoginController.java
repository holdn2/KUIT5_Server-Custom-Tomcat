package controller;

import db.MemoryUserRepository;
import enums.UrlPath;
import enums.UserParam;
import http.HttpRequest;
import http.HttpResponse;
import http.util.HttpRequestUtils;
import model.User;

import java.io.IOException;
import java.util.Map;

public class LoginController implements Controller{
    @Override
    public void execute(HttpRequest request, HttpResponse response) throws Exception {
        String body = request.getBody();
        Map<String, String> params = HttpRequestUtils.parseQueryParameter(body);

        String userId = params.get(UserParam.USER_ID.key());
        String password = params.get(UserParam.PASSWORD.key());

        // id로 User 객체를 찾는다.
        User user = MemoryUserRepository.getInstance().findUserById(userId);

        // 해당 id의 User가 존재하고 입력 비밀번호가 해당 User의 비밀번호와 일치하면 쿠키를 추가하고 redirect한다.
        if (user != null && user.getPassword().equals(password)) {
            response.redirectWithCookie(UrlPath.INDEX.path(), "logined=true");
            return;
        }
        // 조건에 맞지 않으면 login_failed.html로 리다이렉트 시킨다.
        response.redirect(UrlPath.LOGIN_FAILED.path());


    }
}
