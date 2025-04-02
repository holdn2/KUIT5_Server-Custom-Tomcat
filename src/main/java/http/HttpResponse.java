package http;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

public class HttpResponse {
    private final DataOutputStream dos;

    public HttpResponse(OutputStream out) {
        this.dos = new DataOutputStream(out);
    }

    public void forward(String path) throws IOException {
        File file = new File("webapp" + path);
        if (!file.exists()) {
            send404();
            return;
        }

        byte[] body = Files.readAllBytes(file.toPath());
        dos.writeBytes("HTTP/1.1 200 OK\r\n");
        dos.writeBytes("Content-Type: " + getContentType(path) + ";charset=utf-8\r\n");
        dos.writeBytes("Content-Length: " + body.length + "\r\n");
        dos.writeBytes("\r\n");
        dos.write(body);
        dos.flush();
    }

    public void redirect(String path) throws IOException {
        dos.writeBytes("HTTP/1.1 302 Found\r\n");
        dos.writeBytes("Location: " + path + "\r\n");
        dos.writeBytes("\r\n");
        dos.flush();
    }

    public void redirectWithCookie(String path, String cookie) throws IOException {
        dos.writeBytes("HTTP/1.1 302 Found\r\n");
        dos.writeBytes("Location: " + path + "\r\n");
        dos.writeBytes("Set-Cookie: " + cookie + "\r\n");
        dos.writeBytes("\r\n");
        dos.flush();
    }

    private void send404() throws IOException {
        String body = "404 Not Found";
        dos.writeBytes("HTTP/1.1 404 Not Found\r\n");
        dos.writeBytes("Content-Type: text/plain;charset=utf-8\r\n");
        dos.writeBytes("Content-Length: " + body.length() + "\r\n");
        dos.writeBytes("\r\n");
        dos.write(body.getBytes());
        dos.flush();
    }

    private String getContentType(String path) {
        if (path.endsWith(".css")) return "text/css";
        return "text/html";
    }
}
