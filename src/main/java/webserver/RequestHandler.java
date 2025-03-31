package webserver;

import db.MemoryUserRepository;
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
            DataOutputStream dos = new DataOutputStream(out);

            String line = br.readLine();
            System.out.println("요청 라인: " + line);

            if (line == null || line.isEmpty()) return;

            String[] tokens = line.split(" ");
            if (tokens.length < 2) return;

            String method = tokens[0];
            String urlPath = tokens[1];
            if (urlPath.equals("/")) {
                urlPath = "/index.html";
            }

            // 헤더 파싱!
            // body의 내용에 대한 length 가져오기. Content-Length 값
            // 로그인되어있는지 쿠키 유무를 통해 확인한다.
            int contentLength = 0;
            boolean isLoggedIn = false;
            String headerLine;
            while (br.ready() && !(headerLine = br.readLine()).equals("")) {
                if (headerLine.startsWith("Content-Length:")) {
                    contentLength = Integer.parseInt(headerLine.split(":")[1].trim());
                }
                if (headerLine.startsWith("Cookie:")) {
                    String[] cookies = headerLine.substring("Cookie:".length()).trim().split(";");
                    for (String cookie : cookies) {
                        String[] kv = cookie.trim().split("=", 2);
                        if (kv.length == 2 && kv[0].trim().equals("logined")) {
                            isLoggedIn = kv[1].trim().equals("true");
                        }
                    }
                }
            }

            // 요구사항 2번
            if (method.equals("GET") && urlPath.startsWith("/user/signup")) {
                String[] pathSplit = urlPath.split("\\?", 1);
                if (pathSplit.length > 1) {
                    String queryString = pathSplit[1];
                    Map<String, String> params = HttpRequestUtils.parseQueryParameter(queryString);
                    User user = new User(
                            params.get("userId"),
                            params.get("password"),
                            params.get("name"),
                            params.get("email")
                    );
                    MemoryUserRepository.getInstance().addUser(user);
                }
                response302Header(dos, "/index.html");
                return;
            }

            // 요구사항 3번
            if(method.equals("POST")&&urlPath.startsWith("/user/signup")) {
                String body = IOUtils.readData(br, contentLength);
                System.out.println(body);
                if (!body.isEmpty()) {
                    String queryString = body;
                    Map<String, String> params = HttpRequestUtils.parseQueryParameter(queryString);
                    User user = new User(
                            params.get("userId"),
                            params.get("password"),
                            params.get("name"),
                            params.get("email")
                    );
                    System.out.println("생성된 유정 정보 : "+user);
                    MemoryUserRepository.getInstance().addUser(user);
                }
                response302Header(dos, "/index.html");
                return;
            }

            // 요구사항 5번
            if (method.equals("POST") && urlPath.startsWith("/user/login")) {
                String body = IOUtils.readData(br, contentLength);
                Map<String, String> params = HttpRequestUtils.parseQueryParameter(body);

                String userId = params.get("userId");
                String password = params.get("password");

                // id로 User 객체를 찾는다.
                User user = MemoryUserRepository.getInstance().findUserById(userId);

                // 해당 id의 User가 존재하고 입력 비밀번호가 해당 User의 비밀번호와 일치하면 쿠키를 추가하고 redirect한다.
                if (user != null && user.getPassword().equals(password)) {
                    response302HeaderWithCookie(dos, "/index.html", "logined=true");
                } else {
                    response302Header(dos, "/user/login_failed.html");
                }
                return;
            }

            // 요구사항 6
            // 사용자 목록 출력 (요구사항 6)
            if (method.equals("GET") && urlPath.equals("/user/userList")) {
                if (!isLoggedIn) {
                    response302Header(dos, "/user/login.html");
                    return;
                }
                // 정적 파일 userList.html 반환
                File file = new File("webapp/user/list.html");
                if (file.exists() && file.isFile()) {
                    byte[] body = Files.readAllBytes(file.toPath());
                    response200Header(dos, body.length, urlPath);
                    responseBody(dos, body);
                } else {
                    byte[] body = "404 Not Found".getBytes();
                    responseBody(dos, body);
                }
                return;
            }

            File file = new File("webapp" + urlPath);
            System.out.println("요청한 파일 경로: " + file.getAbsolutePath());

            if (file.exists() && file.isFile()) {
                byte[] body = Files.readAllBytes(file.toPath());
                response200Header(dos, body.length,urlPath);
                responseBody(dos, body);
            } else {
                byte[] body = "404 Not Found".getBytes();
                responseBody(dos, body);
            }

        } catch (IOException e) {
            log.log(Level.SEVERE,e.getMessage());
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent, String urlPath) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            String contentType = getContentType(urlPath);
            dos.writeBytes("Content-Type: "+contentType+";charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }

    private String getContentType(String urlPath) {
        if(urlPath.endsWith(".css")) return "text/css";
        return "text/html";
    }

    private void response302Header(DataOutputStream dos, String path) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found\r\n");
            dos.writeBytes("Location: " + path + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }

    private void response302HeaderWithCookie(DataOutputStream dos, String path, String cookie) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found\r\n");
            dos.writeBytes("Location: " + path + "\r\n");
            dos.writeBytes("Set-Cookie: " + cookie + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }


    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.log(Level.SEVERE, e.getMessage());
        }
    }

}
