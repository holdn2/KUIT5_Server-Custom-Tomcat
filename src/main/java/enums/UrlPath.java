package enums;

public enum UrlPath {
    DEFAULT("/"),
    INDEX("/index.html"),

    // 요청 처리용
    SIGNUP("/user/signup"),
    LOGIN("/user/login"),

    USER_LIST("/user/list.html"),

    // 리다이렉션용
    LOGIN_PAGE("/user/login.html"),
    LOGIN_FAILED("/user/login_failed.html");

    private final String path;

    UrlPath(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }
}

