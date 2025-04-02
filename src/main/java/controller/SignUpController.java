package controller;

import db.MemoryUserRepository;
import enums.HttpMethod;
import enums.UserParam;
import http.HttpRequest;
import http.HttpResponse;
import http.util.HttpRequestUtils;
import model.User;

import java.util.Map;

public class SignUpController implements Controller {
    @Override
    public void execute(HttpRequest request, HttpResponse response) throws Exception {
        if (request.isMethod(HttpMethod.GET)) {
            String[] pathSplit = request.getPath().split("\\?", 2);
            if (pathSplit.length > 1) {
                Map<String, String> params = HttpRequestUtils.parseQueryParameter(pathSplit[1]);
                User user = new User(
                        params.get(UserParam.USER_ID.key()),
                        params.get(UserParam.PASSWORD.key()),
                        params.get(UserParam.NAME.key()),
                        params.get(UserParam.EMAIL.key())
                );
                MemoryUserRepository.getInstance().addUser(user);
            }
        } else if (request.isMethod(HttpMethod.POST)) {
            Map<String, String> params = HttpRequestUtils.parseQueryParameter(request.getBody());
            User user = new User(
                    params.get(UserParam.USER_ID.key()),
                    params.get(UserParam.PASSWORD.key()),
                    params.get(UserParam.NAME.key()),
                    params.get(UserParam.EMAIL.key())
            );
            MemoryUserRepository.getInstance().addUser(user);
        }
        response.redirect("/index.html");
    }
}
