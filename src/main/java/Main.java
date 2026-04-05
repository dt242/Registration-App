import com.sun.net.httpserver.HttpServer;
import model.User;
import repository.UserRepository;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static util.HttpUtils.*;
import static util.SecurityUtils.hashPassword;

public class Main {
    private static final Map<String, String> activeSessions = new ConcurrentHashMap<>();
    private static final Map<String, String> activeCaptchas = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/style.css", exchange -> {
            try (InputStream is = Main.class.getClassLoader().getResourceAsStream("css/style.css")) {
                if (is == null) {
                    sendResponse(exchange, 404, "text/plain; charset=UTF-8", "CSS not found");
                    return;
                }
                String cssContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                sendResponse(exchange, 200, "text/css; charset=UTF-8", cssContent);
            }
        });

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
                try (InputStream is = Main.class.getClassLoader().getResourceAsStream("html/register.html")) {
                    if (is == null) {
                        sendErrorPage(exchange, 404, "System file not found! Please, contact administration.");
                        return;
                    }
                    String htmlContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    sendResponse(exchange, 200, "text/html; charset=UTF-8", htmlContent);
                }

            } else if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String rawFormData = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> parsedData = parseFormData(rawFormData);
                String firstName = parsedData.get("firstName");
                String lastName = parsedData.get("lastName");
                String email = parsedData.get("email");
                String rawPassword = parsedData.get("password");
                String userCaptcha = parsedData.get("captcha");

                if (firstName == null || firstName.trim().isEmpty() ||
                        lastName == null || lastName.trim().isEmpty() ||
                        email == null || email.trim().isEmpty() ||
                        rawPassword == null || rawPassword.trim().isEmpty() ||
                        userCaptcha == null || userCaptcha.trim().isEmpty()) {
                    sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\": false, \"message\": \"All fields are required!\"}");
                    return;
                }
                String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
                if (!email.matches(emailRegex)) {
                    sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\": false, \"message\": \"Please, enter a valid email!\"}");
                    return;
                }
                if (rawPassword.length() < 6) {
                    sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\": false, \"message\": \"Password must be at least 6 characters!\"}");
                    return;
                }

                String currentCaptchaId = getCookieValue(exchange, "captcha_id");
                String realCaptchaText = activeCaptchas.get(currentCaptchaId);
                if (realCaptchaText == null || !realCaptchaText.equalsIgnoreCase(userCaptcha)) {
                    sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\": false, \"message\": \"Wrong text from the image! Try again.\"}");
                    return;
                }
                activeCaptchas.remove(currentCaptchaId);

                try {
                    if (UserRepository.emailExists(email)) {
                        sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\": false, \"message\": \"Email is already taken!\"}");
                        return;
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    sendResponse(exchange, 500, "application/json; charset=UTF-8", "{\"success\": false, \"message\": \"Server error!\"}");
                    return;
                }

                String hashedPassword = hashPassword(rawPassword);
                try {
                    UserRepository.createUser(firstName,lastName, email, hashedPassword);
                    sendResponse(exchange, 200, "application/json; charset=UTF-8", "{\"success\": true, \"message\": \"Registration was successful! Redirecting...\"}");
                } catch (SQLException e) {
                    e.printStackTrace();
                    sendResponse(exchange, 500, "application/json; charset=UTF-8", "{\"success\": false, \"message\": \"Server error!\"}");
                }
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
                try (InputStream is = Main.class.getClassLoader().getResourceAsStream("html/login.html")) {
                    if (is == null) {
                        sendErrorPage(exchange, 404, "System file not found! Please, contact administration.");
                        return;
                    }
                    String htmlContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    sendResponse(exchange, 200, "text/html; charset=UTF-8", htmlContent);
                }

            } else if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String rawFormData = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> parsedData = parseFormData(rawFormData);
                String email = parsedData.get("email");
                String rawPassword = parsedData.get("password");
                String hashedPassword = hashPassword(rawPassword);
                try {
                    if (UserRepository.validateCredentials(email, hashedPassword)) {
                        String sessionToken = UUID.randomUUID().toString();
                        activeSessions.put(sessionToken, email);
                        String cookieString = "session_token=" + sessionToken + "; HttpOnly; Path=/";
                        exchange.getResponseHeaders().add("Set-Cookie", cookieString);
                        sendResponse(exchange, 200, "application/json; charset=UTF-8", "{\"success\": true, \"message\": \"Login was successful!\"}");
                    } else {
                        sendResponse(exchange, 401, "application/json; charset=UTF-8", "{\"success\": false, \"message\": \"Wrong email or password!\"}");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    sendResponse(exchange, 500, "application/json; charset=UTF-8", "{\"success\": false, \"message\": \"Server error!\"}");
                }
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
                User user;
                try {
                    user = UserRepository.getUserByEmail(userEmail);
                } catch (SQLException e) {
                    e.printStackTrace();
                    sendErrorPage(exchange, 500, "Database error!");
                    return;
                }
                if (user == null) {
                    sendErrorPage(exchange, 404, "User not found in database!");
                    return;
                }
                try (InputStream is = Main.class.getClassLoader().getResourceAsStream("html/profile.html")) {
                    if (is == null) {
                        sendErrorPage(exchange, 404, "System file not found! Please, contact administration.");
                        return;
                    }
                    String htmlTemplate = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    String finalHtml = htmlTemplate
                            .replace("{{email}}", userEmail)
                            .replace("{{firstName}}", user.getFirstName())
                            .replace("{{lastName}}", user.getLastName());
                    sendResponse(exchange, 200, "text/html; charset=UTF-8", finalHtml);
                }

            } else if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String rawFormData = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> parsedData = parseFormData(rawFormData);
                String newFirstName = parsedData.get("firstName");
                String newLastName = parsedData.get("lastName");
                String newPassword = parsedData.get("password");
                if (newFirstName == null || newFirstName.trim().isEmpty() ||
                        newLastName == null || newLastName.trim().isEmpty()) {
                    sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\": false, \"message\": \"First and Last names cannot be empty!\"}");
                    return;
                }
                if (newPassword != null && !newPassword.trim().isEmpty() && newPassword.length() < 6) {
                    sendResponse(exchange, 400, "application/json; charset=UTF-8", "{\"success\": false, \"message\": \"New password must be at least 6 characters!\"}");
                    return;
                }
                try {
                    UserRepository.updateProfile(userEmail, newFirstName, newLastName, newPassword);
                    sendResponse(exchange, 200, "application/json; charset=UTF-8", "{\"success\": true}");
                } catch (SQLException e) {
                    e.printStackTrace();
                    sendResponse(exchange, 500, "application/json; charset=UTF-8", "{\"success\": false, \"message\": \"Database error!\"}");
                }
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
                g2d.drawOval(x1, y1, ovalWidth, ovalHeight);
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
}