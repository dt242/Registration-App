package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import core.SessionManager;
import util.HttpUtils;

import java.io.IOException;

public class LogoutHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestURI().getPath().equals("/logout")) {
            HttpUtils.sendErrorPage(exchange, 404, "The page you are looking for has been moved or does not exist.");
            return;
        }
        String token = HttpUtils.getCookieValue(exchange, "session_token");
        SessionManager.invalidateSession(token);
        String killCookie = "session_token=; HttpOnly; Path=/; Max-Age=0";
        exchange.getResponseHeaders().add("Set-Cookie", killCookie);
        exchange.getResponseHeaders().add("Location", "/login");
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }
}