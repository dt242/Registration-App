import com.sun.net.httpserver.HttpServer;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    private static final Map<String, String> activeSessions = new ConcurrentHashMap<>();
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/", exchange -> {
            String response = "The server is working";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        });

        server.createContext("/register", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                InputStream is = Main.class.getClassLoader().getResourceAsStream("html/register.html");
                if (is == null) {
                    String error = "404 - File not found!";
                    exchange.sendResponseHeaders(404, error.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(error.getBytes());
                    os.close();
                    return;
                }
                byte[] response = is.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.close();
            } else if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String rawFormData = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> parsedData = parseFormData(rawFormData);
                String firstName = parsedData.get("firstName");
                String lastName = parsedData.get("lastName");
                String email = parsedData.get("email");
                String rawPassword = parsedData.get("password");

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

                exchange.sendResponseHeaders(200, responseText.length());
                OutputStream os = exchange.getResponseBody();
                os.write(responseText.getBytes());
                os.close();
            }
        });

        server.createContext("/login", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                InputStream is = Main.class.getClassLoader().getResourceAsStream("html/login.html");
                if (is == null) {
                    String error = "404 - File not found!";
                    exchange.sendResponseHeaders(404, error.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(error.getBytes());
                    os.close();
                    return;
                }
                byte[] response = is.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                OutputStream os = exchange.getResponseBody();
                os.write(response);
                os.close();
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
                exchange.sendResponseHeaders(200, responseText.length());
                OutputStream os = exchange.getResponseBody();
                os.write(responseText.getBytes());
                os.close();
            }
        });

        server.setExecutor(null);
        server.start();
    }

    private static java.util.Map<String, String> parseFormData(String formData) throws UnsupportedEncodingException {
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
}