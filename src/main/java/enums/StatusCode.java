package enums;

public enum StatusCode {
    OK("200 OK"),
    FOUND("302 Found"),
    NOT_FOUND("404 Not Found");

    private final String message;

    StatusCode(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}

