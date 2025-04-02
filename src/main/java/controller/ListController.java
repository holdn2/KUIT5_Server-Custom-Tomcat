package controller;

import enums.UrlPath;
import http.HttpRequest;
import http.HttpResponse;

public class ListController implements Controller{
    @Override
    public void execute(HttpRequest request, HttpResponse response) throws Exception{
        if (!request.isLogined()) {
            response.redirect(UrlPath.LOGIN_PAGE.path());
        }
        response.forward(UrlPath.USER_LIST.path());
    }
}
