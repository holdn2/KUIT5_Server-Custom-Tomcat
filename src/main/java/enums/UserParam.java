package enums;

public enum UserParam {
    USER_ID("userId"),
    PASSWORD("password"),
    NAME("name"),
    EMAIL("email");

    private final String key;

    UserParam(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}

