package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import core.SessionManager;
import repository.UserRepository;
import util.HttpUtils;
import util.SecurityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;

public class LoginHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestURI().getPath().equals("/login")) {
            HttpUtils.sendErrorPage(exchange, 404, "The page you are looking for has been moved or does not exist.");
            return;
        }
        String token = HttpUtils.getCookieValue(exchange, "session_token");
        if (SessionManager.isSessionValid(token)) {
            exchange.getResponseHeaders().add("Location", "/profile");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
            return;
        }
        if ("GET".equals(exchange.getRequestMethod())) {
            handleGet(exchange);
        } else if ("POST".equals(exchange.getRequestMethod())) {
            handlePost(exchange);
        } else {
            HttpUtils.sendErrorPage(exchange, 405, "Method Not Allowed");
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("html/login.html")) {
            if (is == null) {
                HttpUtils.sendErrorPage(exchange, 404, "System file not found! Please, contact administration.");
                return;
            }
            String htmlContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            HttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", htmlContent);
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        String rawFormData = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> parsedData = HttpUtils.parseFormData(rawFormData);
        String email = parsedData.get("email");
        String rawPassword = parsedData.get("password");
        if (email == null || email.trim().isEmpty() || rawPassword == null || rawPassword.trim().isEmpty()) {
            HttpUtils.sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\": false, \"message\": \"Email and password are required!\"}");
            return;
        }
        String hashedPassword = SecurityUtils.hashPassword(rawPassword);
        try {
            if (UserRepository.validateCredentials(email, hashedPassword)) {
                String cookieString = "session_token=" + SessionManager.createSession(email) + "; HttpOnly; Path=/";
                exchange.getResponseHeaders().add("Set-Cookie", cookieString);
                HttpUtils.sendResponse(exchange, 200, "application/json; charset=UTF-8", "{\"success\": true, \"message\": \"Login was successful!\"}");
            } else {
                HttpUtils.sendResponse(exchange, 401, "application/json; charset=UTF-8", "{\"success\": false, \"message\": \"Wrong email or password!\"}");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            HttpUtils.sendResponse(exchange, 500, "application/json; charset=UTF-8", "{\"success\": false, \"message\": \"Server error!\"}");
        }
    }
}