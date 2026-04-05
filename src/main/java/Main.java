import com.sun.net.httpserver.HttpServer;
import handler.*;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/style.css", new StaticHandler());
        server.createContext("/", new RootHandler());
        server.createContext("/register", new RegisterHandler());
        server.createContext("/login", new LoginHandler());
        server.createContext("/logout", new LogoutHandler());
        server.createContext("/profile", new ProfileHandler());
        server.createContext("/captcha", new CaptchaHandler());

        server.setExecutor(null);
        server.start();
    }
}