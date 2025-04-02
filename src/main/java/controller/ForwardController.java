package controller;

import http.HttpRequest;
import http.HttpResponse;

public class ForwardController implements Controller {

    @Override
    public void execute(HttpRequest request, HttpResponse response) throws Exception {
        // 기본적으로 요청한 URL 경로 그대로 forward
        response.forward(request.getPath());
    }
}
