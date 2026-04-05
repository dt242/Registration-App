import com.sun.net.httpserver.HttpServer;
import handler.*;

import java.io.IOException;
import java.net.InetSocketAddress;

import static util.HttpUtils.*;

public class Main {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/style.css", new StaticHandler());
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

        server.createContext("/register", new RegisterHandler());
        server.createContext("/login", new LoginHandler());
        server.createContext("/logout", new LogoutHandler());
        server.createContext("/profile", new ProfileHandler());
        server.createContext("/captcha", new CaptchaHandler());

        server.setExecutor(null);
        server.start();
    }
}