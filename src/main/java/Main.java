import com.sun.net.httpserver.HttpServer;
import core.SessionManager;
import handler.ProfileHandler;
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

import static util.HttpUtils.*;
import static util.SecurityUtils.hashPassword;

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

        server.createContext("/register", new handler.RegisterHandler());
        server.createContext("/login", new handler.LoginHandler());

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
                        SessionManager.invalidateSession(token);
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

        server.createContext("/profile", new ProfileHandler());

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
            String cookieString = "captcha_id=" + SessionManager.saveCaptcha(captchaText.toString()) + "; HttpOnly; Path=/";
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