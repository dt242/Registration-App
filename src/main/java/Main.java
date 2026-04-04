import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    private static final Map<String, String> activeSessions = new ConcurrentHashMap<>();
    private static final Map<String, String> activeCaptchas = new ConcurrentHashMap<>();
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                exchange.getResponseHeaders().add("Location", "/login");
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
            } else {
                sendErrorPage(exchange, 404, "The page you are looking for has been moved or does not exist.");
            }
        });

        server.createContext("/register", exchange -> {
            if (!exchange.getRequestURI().getPath().equals("/register")) {
                sendErrorPage(exchange, 404, "The page you are looking for has been moved or does not exist.");
                return;
            }

            String token = getCookieValue(exchange, "session_token");
            if (token != null && activeSessions.containsKey(token)) {
                exchange.getResponseHeaders().add("Location", "/profile");
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
                return;
            }

            if ("GET".equals(exchange.getRequestMethod())) {
                InputStream is = Main.class.getClassLoader().getResourceAsStream("html/register.html");
                if (is == null) {
                    sendErrorPage(exchange, 404, "System file not found! Please contact administration.");
                    return;
                }
                String htmlContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                sendResponse(exchange, 200, "text/html; charset=UTF-8", htmlContent);

            } else if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String rawFormData = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> parsedData = parseFormData(rawFormData);
                String firstName = parsedData.get("firstName");
                String lastName = parsedData.get("lastName");
                String email = parsedData.get("email");
                String rawPassword = parsedData.get("password");

                String userCaptcha = parsedData.get("captcha");
                String currentCaptchaId = getCookieValue(exchange, "captcha_id");
                String realCaptchaText = activeCaptchas.get(currentCaptchaId);
                if (realCaptchaText == null || !realCaptchaText.equalsIgnoreCase(userCaptcha)) {
                    sendResponse(exchange, 400, "text/plain; charset=UTF-8", "Wrong text! Try again.");
                    return;
                }
                activeCaptchas.remove(currentCaptchaId);

                try (Connection connection = DatabaseManager.getConnection()) {
                    String checkSql = "SELECT COUNT(*) FROM users WHERE email = ?";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(checkSql)) {
                        preparedStatement.setString(1, email);
                        try (ResultSet resultSet = preparedStatement.executeQuery()) {
                            if (resultSet.next() && resultSet.getInt(1) > 0) {
                                sendResponse(exchange, 400, "text/plain; charset=UTF-8", "Email is already taken!");
                                return;
                            }
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    sendResponse(exchange, 500, "text/plain; charset=UTF-8", "Internal server error!");
                    return;
                }

                String hashedPassword = hashPassword(rawPassword);
                String responseText;
                try (Connection connection = DatabaseManager.getConnection()) {
                    String sql = "INSERT INTO users (first_name, last_name, email, password_hash) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                        preparedStatement.setString(1, firstName);
                        preparedStatement.setString(2, lastName);
                        preparedStatement.setString(3, email);
                        preparedStatement.setString(4, hashedPassword);
                        preparedStatement.executeUpdate();
                        responseText = "Registration was successful! Hello, " + firstName;
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    responseText = "Error!";
                }
                sendResponse(exchange, 200, "text/plain; charset=UTF-8", responseText);

            }
        });

        server.createContext("/login", exchange -> {
            if (!exchange.getRequestURI().getPath().equals("/login")) {
                sendErrorPage(exchange, 404, "The page you are looking for has been moved or does not exist.");
                return;
            }

            String token = getCookieValue(exchange, "session_token");
            if (token != null && activeSessions.containsKey(token)) {
                exchange.getResponseHeaders().add("Location", "/profile");
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
                return;
            }

            if ("GET".equals(exchange.getRequestMethod())) {
                InputStream is = Main.class.getClassLoader().getResourceAsStream("html/login.html");
                if (is == null) {
                    sendErrorPage(exchange, 404, "System file not found! Please contact administration.");
                    return;
                }
                String htmlContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                sendResponse(exchange, 200, "text/html; charset=UTF-8", htmlContent);

            } else if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String rawFormData = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> parsedData = parseFormData(rawFormData);
                String email = parsedData.get("email");
                String rawPassword = parsedData.get("password");
                String hashedPassword = hashPassword(rawPassword);
                String responseText;
                try (Connection connection = DatabaseManager.getConnection()) {
                    String sql = "SELECT first_name FROM users WHERE email = ? AND password_hash = ?";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                        preparedStatement.setString(1, email);
                        preparedStatement.setString(2, hashedPassword);
                        try (ResultSet resultSet = preparedStatement.executeQuery()) {
                            if (resultSet.next()) {
                                String firstName = resultSet.getString("first_name");
                                String sessionToken = UUID.randomUUID().toString();
                                activeSessions.put(sessionToken, email);
                                String cookieString = "session_token=" + sessionToken + "; HttpOnly; Path=/";
                                exchange.getResponseHeaders().add("Set-Cookie", cookieString);
                                responseText = "Login was successful! Welcome back, " + firstName;
                            } else {
                                responseText = "Login was unsuccessful!";
                            }
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    responseText = "Error!";
                }
                sendResponse(exchange, 200, "text/plain; charset=UTF-8", responseText);
            }
        });

        server.createContext("/logout", exchange -> {
            if (!exchange.getRequestURI().getPath().equals("/logout")) {
                sendErrorPage(exchange, 404, "The page you are looking for has been moved or does not exist.");
                return;
            }
            List<String> cookies = exchange.getRequestHeaders().get("Cookie");
            if (cookies != null) {
                for (String cookie : cookies) {
                    if (cookie.contains("session_token=")) {
                        String token = cookie.split("session_token=")[1].split(";")[0];
                        activeSessions.remove(token);
                        break;
                    }
                }
            }
            String killCookie = "session_token=; HttpOnly; Path=/; Max-Age=0";
            exchange.getResponseHeaders().add("Set-Cookie", killCookie);
            exchange.getResponseHeaders().add("Location", "/login");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });

        server.createContext("/profile", exchange -> {
            if (!exchange.getRequestURI().getPath().equals("/profile")) {
                sendErrorPage(exchange, 404, "The page you are looking for has been moved or does not exist.");
                return;
            }
            String token = getCookieValue(exchange, "session_token");
            String userEmail = (token != null) ? activeSessions.get(token) : null;
            if (userEmail == null) {
                exchange.getResponseHeaders().add("Location", "/login");
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
                return;
            }

            if ("GET".equals(exchange.getRequestMethod())) {
                String currentFirstName = "";
                String currentLastName = "";
                try (Connection connection = DatabaseManager.getConnection();
                     PreparedStatement preparedStatement = connection.prepareStatement("SELECT first_name, last_name FROM users WHERE email = ?")) {
                    preparedStatement.setString(1, userEmail);
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        if (resultSet.next()) {
                            currentFirstName = resultSet.getString("first_name");
                            currentLastName = resultSet.getString("last_name");
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                InputStream is = Main.class.getClassLoader().getResourceAsStream("html/profile.html");
                if (is == null) {
                    sendErrorPage(exchange, 404, "System file not found! Please contact administration.");
                    return;
                }
                String htmlTemplate = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                String finalHtml = htmlTemplate
                        .replace("{{email}}", userEmail)
                        .replace("{{firstName}}", currentFirstName)
                        .replace("{{lastName}}", currentLastName);
                sendResponse(exchange, 200, "text/html; charset=UTF-8", finalHtml);

            } else if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String rawFormData = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> parsedData = parseFormData(rawFormData);
                String newFirstName = parsedData.get("firstName");
                String newLastName = parsedData.get("lastName");
                String newPassword = parsedData.get("password");
                try (Connection connection = DatabaseManager.getConnection()) {
                    if (newPassword != null && !newPassword.trim().isEmpty()) {
                        String sql = "UPDATE users SET first_name = ?, last_name = ?, password_hash = ? WHERE email = ?";
                        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                            preparedStatement.setString(1, newFirstName);
                            preparedStatement.setString(2, newLastName);
                            preparedStatement.setString(3, hashPassword(newPassword));
                            preparedStatement.setString(4, userEmail);
                            preparedStatement.executeUpdate();
                        }
                    } else {
                        String sql = "UPDATE users SET first_name = ?, last_name = ? WHERE email = ?";
                        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                            preparedStatement.setString(1, newFirstName);
                            preparedStatement.setString(2, newLastName);
                            preparedStatement.setString(3, userEmail);
                            preparedStatement.executeUpdate();
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                sendResponse(exchange, 200, "application/json; charset=UTF-8", "{\"success\": true}");
            }
        });

        server.createContext("/captcha", exchange -> {
            if (!exchange.getRequestURI().getPath().equals("/captcha")) {
                sendErrorPage(exchange, 404, "The page you are looking for has been moved or does not exist.");
                return;
            }
            String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
            StringBuilder captchaText = new StringBuilder();
            Random rnd = new Random();
            for (int i = 0; i < 5; i++) {
                captchaText.append(chars.charAt(rnd.nextInt(chars.length())));
            }
            String captchaId = UUID.randomUUID().toString();
            activeCaptchas.put(captchaId, captchaText.toString());
            String cookieString = "captcha_id=" + captchaId + "; HttpOnly; Path=/";
            exchange.getResponseHeaders().add("Set-Cookie", cookieString);
            int width = 160;
            int height = 50;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, width, height);
            g2d.setColor(Color.DARK_GRAY);
            for (int i = 0; i < 8; i++) {
                int x1 = rnd.nextInt(width);
                int y1 = rnd.nextInt(height);
                int x2 = rnd.nextInt(width);
                int y2 = rnd.nextInt(height);
                g2d.setStroke(new BasicStroke(rnd.nextFloat() * 1.5f + 0.5f));
                g2d.drawLine(x1, y1, x2, y2);
            }
            for (int i = 0; i < 5; i++) {
                int x1 = rnd.nextInt(width);
                int y1 = rnd.nextInt(height);
                int ovalWidth = rnd.nextInt(width);
                int ovalHeight = rnd.nextInt(height);
                g2d.setStroke(new BasicStroke(rnd.nextFloat() * 1.5f + 0.5f));
                g2d.drawOval(x1, y1,ovalWidth, ovalHeight);
            }
            g2d.setStroke(new BasicStroke(1.0f));
            g2d.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 35));
            g2d.setColor(Color.DARK_GRAY);
            for (int i = 0; i < captchaText.length(); i++) {
                AffineTransform affineTransform = new AffineTransform();
                affineTransform.rotate(rnd.nextDouble() * 0.4 - 0.2, 0, 0);
                Font rotatedFont = g2d.getFont().deriveFont(affineTransform);
                g2d.setFont(rotatedFont);
                g2d.drawString(String.valueOf(captchaText.charAt(i)), 20 + (i * 25), 35);
            }
            g2d.dispose();
            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, 0);
            ImageIO.write(image, "png", exchange.getResponseBody());
            exchange.close();
        });

        server.setExecutor(null);
        server.start();
    }

    private static Map<String, String> parseFormData(String formData) throws UnsupportedEncodingException {
        Map<String, String> map = new HashMap<>();
        String[] pairs = formData.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                map.put(key, value);
            }
        }
        return map;
    }

    private static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error during encryption!", e);
        }
    }

    private static String getCookieValue(HttpExchange exchange, String cookieName) {
        List<String> cookies = exchange.getRequestHeaders().get("Cookie");
        if (cookies != null) {
            for (String cookie : cookies) {
                if (cookie.contains(cookieName + "=")) {
                    return cookie.split(cookieName + "=")[1].split(";")[0];
                }
            }
        }
        return null;
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String contentType, String responseText) throws IOException {
        byte[] responseBytes = responseText.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }

    private static void sendErrorPage(HttpExchange exchange, int statusCode, String message) throws IOException {
        InputStream is = Main.class.getClassLoader().getResourceAsStream("html/error.html");
        if (is == null) {
            sendResponse(exchange, statusCode, "text/plain; charset=UTF-8", statusCode + " - " + message);
            return;
        }
        String htmlTemplate = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        String finalHtml = htmlTemplate
                .replace("{{statusCode}}", String.valueOf(statusCode))
                .replace("{{errorMessage}}", message);
        sendResponse(exchange, statusCode, "text/html; charset=UTF-8", finalHtml);
    }
}