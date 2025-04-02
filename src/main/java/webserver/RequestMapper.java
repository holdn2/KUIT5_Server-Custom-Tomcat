package webserver;

import controller.*;
import http.HttpRequest;
import http.HttpResponse;

import java.util.HashMap;
import java.util.Map;

public class RequestMapper {
    private final HttpRequest request;
    private final HttpResponse response;
    private final Map<String, Controller> controllers = new HashMap<>();

    public RequestMapper(HttpRequest request, HttpResponse response) {
        this.request = request;
        this.response = response;
        initControllerMapping();
    }

    private void initControllerMapping() {
        controllers.put("/", new HomeController());
        controllers.put("/index.html", new HomeController());
        controllers.put("/user/signup", new SignUpController());
        controllers.put("/user/login", new LoginController());
        controllers.put("/user/userList", new ListController());
    }

    public void proceed() throws Exception {
        String path = request.getPath();
        // getOrDefault 함수는 path를 키로 하는 것이 없다면 뒤에 있는 것을 반환하고
        // 키로 path를 가지고 있다면 해당 값을 반환한다.4
        // 등록된 path가 들어오지 않는다면 ForwardController로
        Controller controller = controllers.getOrDefault(path, new ForwardController());
        controller.execute(request, response);
    }
}
