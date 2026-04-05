package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import core.SessionManager;
import model.User;
import repository.UserRepository;
import util.HttpUtils;
import util.SecurityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;

public class ProfileHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestURI().getPath().equals("/profile")) {
            HttpUtils.sendErrorPage(exchange, 404, "The page you are looking for has been moved or does not exist.");
            return;
        }
        String token = HttpUtils.getCookieValue(exchange, "session_token");
        String userEmail = SessionManager.getEmailByToken(token);
        if (userEmail == null) {
            exchange.getResponseHeaders().add("Location", "/login");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
            return;
        }
        if ("GET".equals(exchange.getRequestMethod())) {
            handleGet(exchange, userEmail);
        } else if ("POST".equals(exchange.getRequestMethod())) {
            handlePost(exchange, userEmail);
        } else {
            HttpUtils.sendErrorPage(exchange, 405, "Method Not Allowed");
        }
    }

    private void handleGet(HttpExchange exchange, String userEmail) throws IOException {
        User user;
        try {
            user = UserRepository.getUserByEmail(userEmail);
        } catch (SQLException e) {
            e.printStackTrace();
            HttpUtils.sendErrorPage(exchange, 500, "Database error!");
            return;
        }
        if (user == null) {
            HttpUtils.sendErrorPage(exchange, 404, "User not found in database!");
            return;
        }
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("html/profile.html")) {
            if (is == null) {
                HttpUtils.sendErrorPage(exchange, 404, "System file not found! Please, contact administration.");
                return;
            }
            String htmlTemplate = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            String finalHtml = htmlTemplate
                    .replace("{{email}}", userEmail)
                    .replace("{{firstName}}", user.getFirstName())
                    .replace("{{lastName}}", user.getLastName());
            HttpUtils.sendResponse(exchange, 200, "text/html; charset=UTF-8", finalHtml);
        }
    }

    private void handlePost(HttpExchange exchange, String userEmail) throws IOException {
        InputStream is = exchange.getRequestBody();
        String rawFormData = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> parsedData = HttpUtils.parseFormData(rawFormData);
        String newFirstName = parsedData.get("firstName");
        String newLastName = parsedData.get("lastName");
        String newPassword = parsedData.get("password");
        if (newFirstName == null || newFirstName.trim().isEmpty() ||
                newLastName == null || newLastName.trim().isEmpty()) {
            HttpUtils.sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\": false, \"message\": \"First and Last names cannot be empty!\"}");
            return;
        }
        if (newPassword != null && !newPassword.trim().isEmpty() && newPassword.length() < 6) {
            HttpUtils.sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\": false, \"message\": \"New password must be at least 6 characters!\"}");
            return;
        }
        String hashedNewPassword = null;
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            hashedNewPassword = SecurityUtils.hashPassword(newPassword);
        }
        try {
            UserRepository.updateProfile(userEmail, newFirstName, newLastName, hashedNewPassword);
            HttpUtils.sendResponse(exchange, 200, "application/json; charset=UTF-8", "{\"success\": true}");
        } catch (SQLException e) {
            e.printStackTrace();
            HttpUtils.sendResponse(exchange, 500, "application/json; charset=UTF-8", "{\"success\": false, \"message\": \"Database error!\"}");
        }
    }
}