package util;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpUtils {
    public static Map<String, String> parseFormData(String formData) {
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

    public static String getCookieValue(HttpExchange exchange, String cookieName) {
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

    public static void sendResponse(HttpExchange exchange, int statusCode, String contentType, String responseText) throws IOException {
        byte[] responseBytes = responseText.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        if (contentType.contains("text/html") || contentType.contains("application/json")) {
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            exchange.getResponseHeaders().set("Pragma", "no-cache");
            exchange.getResponseHeaders().set("Expires", "0");
        }
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    public static void sendErrorPage(HttpExchange exchange, int statusCode, String message) throws IOException {
        try (InputStream is = HttpUtils.class.getClassLoader().getResourceAsStream("html/error.html")) {
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
}