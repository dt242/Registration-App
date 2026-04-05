import com.sun.net.httpserver.HttpServer;
import core.SessionManager;
import handler.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static util.HttpUtils.*;

public class Main {

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

        server.createContext("/register", new RegisterHandler());
        server.createContext("/login", new LoginHandler());
        server.createContext("/logout", new LogoutHandler());
        server.createContext("/profile", new ProfileHandler());
        server.createContext("/captcha", new CaptchaHandler());

        server.setExecutor(null);
        server.start();
    }
}