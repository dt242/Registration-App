package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import util.HttpUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class StaticHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestURI().getPath().equals("/style.css")) {
            HttpUtils.sendErrorPage(exchange, 404, "Not Found");
            return;
        }
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("css/style.css")) {
            if (is == null) {
                HttpUtils.sendResponse(exchange, 404, "text/plain; charset=UTF-8", "CSS not found");
                return;
            }
            String cssContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            HttpUtils.sendResponse(exchange, 200, "text/css; charset=UTF-8", cssContent);
        }
    }
}