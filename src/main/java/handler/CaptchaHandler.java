package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import core.SessionManager;
import util.HttpUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

public class CaptchaHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestURI().getPath().equals("/captcha")) {
            HttpUtils.sendErrorPage(exchange, 404, "The page you are looking for has been moved or does not exist.");
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            HttpUtils.sendErrorPage(exchange, 405, "Method Not Allowed");
            return;
        }
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder captchaText = new StringBuilder();
        Random rnd = new Random();
        for (int i = 0; i < 5; i++) {
            captchaText.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        String captchaId = SessionManager.saveCaptcha(captchaText.toString());
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
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
        exchange.getResponseHeaders().set("Pragma", "no-cache");
        exchange.getResponseHeaders().set("Expires", "0");
        exchange.sendResponseHeaders(200, 0);
        ImageIO.write(image, "png", exchange.getResponseBody());
        exchange.close();
    }
}