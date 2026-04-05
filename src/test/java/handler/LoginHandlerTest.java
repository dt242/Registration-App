package handler;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import core.SessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import repository.UserRepository;
import util.HttpUtils;
import util.SecurityUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LoginHandlerTest {

    private HttpExchange mockExchange;
    private Headers mockHeaders;
    private MockedStatic<HttpUtils> mockedHttpUtils;
    private MockedStatic<SessionManager> mockedSessionManager;
    private MockedStatic<UserRepository> mockedUserRepository;
    private MockedStatic<SecurityUtils> mockedSecurityUtils;
    private LoginHandler loginHandler;

    @BeforeEach
    void setUp() {
        mockExchange = mock(HttpExchange.class);
        mockHeaders = mock(Headers.class);
        mockedHttpUtils = mockStatic(HttpUtils.class);
        mockedSessionManager = mockStatic(SessionManager.class);
        mockedUserRepository = mockStatic(UserRepository.class);
        mockedSecurityUtils = mockStatic(SecurityUtils.class);
        loginHandler = new LoginHandler();
        when(mockExchange.getResponseHeaders()).thenReturn(mockHeaders);
    }

    @AfterEach
    void tearDown() {
        mockedHttpUtils.close();
        mockedSessionManager.close();
        mockedUserRepository.close();
        mockedSecurityUtils.close();
    }

    @Test
    void testHandleWrongPath() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/wrongpath"));
        loginHandler.handle(mockExchange);

        mockedHttpUtils.verify(() -> HttpUtils.sendErrorPage(eq(mockExchange), eq(404), eq("The page you are looking for has been moved or does not exist.")));
    }

    @Test
    void testHandleAlreadyLoggedIn() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/login"));
        mockedHttpUtils.when(() -> HttpUtils.getCookieValue(mockExchange, "session_token")).thenReturn("valid_token");
        mockedSessionManager.when(() -> SessionManager.isSessionValid("valid_token")).thenReturn(true);
        loginHandler.handle(mockExchange);

        verify(mockHeaders).add("Location", "/profile");
        verify(mockExchange).sendResponseHeaders(302, -1);
        verify(mockExchange).close();
    }

    @Test
    void testHandleInvalidMethod() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/login"));
        when(mockExchange.getRequestMethod()).thenReturn("PUT");
        mockedHttpUtils.when(() -> HttpUtils.getCookieValue(mockExchange, "session_token")).thenReturn(null);
        loginHandler.handle(mockExchange);

        mockedHttpUtils.verify(() -> HttpUtils.sendErrorPage(eq(mockExchange), eq(405), eq("Method Not Allowed")));
    }

    @Test
    void testHandleGetSuccessfulPageLoad() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/login"));
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        mockedHttpUtils.when(() -> HttpUtils.getCookieValue(mockExchange, "session_token")).thenReturn(null);
        loginHandler.handle(mockExchange);

        mockedHttpUtils.verify(() -> HttpUtils.sendResponse(eq(mockExchange), eq(200), eq("text/html; charset=UTF-8"), anyString()));
    }

    @Test
    void testHandlePostEmptyFields() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/login"));
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        InputStream inputStream = new ByteArrayInputStream("email=&password=".getBytes(StandardCharsets.UTF_8));
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        Map<String, String> parsedData = new HashMap<>();
        parsedData.put("email", "");
        parsedData.put("password", null);
        mockedHttpUtils.when(() -> HttpUtils.parseFormData(anyString())).thenReturn(parsedData);
        loginHandler.handle(mockExchange);

        mockedHttpUtils.verify(() -> HttpUtils.sendResponse(eq(mockExchange), eq(400), eq("application/json; charset=UTF-8"), eq("{\"success\": false, \"message\": \"Email and password are required!\"}")));
    }

    @Test
    void testHandlePostValidCredentials() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/login"));
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        InputStream inputStream = new ByteArrayInputStream("email=test@test.com&password=password123".getBytes(StandardCharsets.UTF_8));
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        Map<String, String> parsedData = new HashMap<>();
        parsedData.put("email", "test@test.com");
        parsedData.put("password", "password123");
        mockedHttpUtils.when(() -> HttpUtils.parseFormData(anyString())).thenReturn(parsedData);
        mockedSecurityUtils.when(() -> SecurityUtils.hashPassword("password123")).thenReturn("hashedPass");
        mockedUserRepository.when(() -> UserRepository.validateCredentials("test@test.com", "hashedPass")).thenReturn(true);
        mockedSessionManager.when(() -> SessionManager.createSession("test@test.com")).thenReturn("new_session_token");
        loginHandler.handle(mockExchange);

        verify(mockHeaders).add("Set-Cookie", "session_token=new_session_token; HttpOnly; Path=/");
        mockedHttpUtils.verify(() -> HttpUtils.sendResponse(eq(mockExchange), eq(200), eq("application/json; charset=UTF-8"), eq("{\"success\": true, \"message\": \"Login was successful!\"}")));
    }

    @Test
    void testHandlePostInvalidCredentials() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/login"));
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        InputStream inputStream = new ByteArrayInputStream("email=test@test.com&password=wrong".getBytes(StandardCharsets.UTF_8));
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        Map<String, String> parsedData = new HashMap<>();
        parsedData.put("email", "test@test.com");
        parsedData.put("password", "wrong");
        mockedHttpUtils.when(() -> HttpUtils.parseFormData(anyString())).thenReturn(parsedData);
        mockedSecurityUtils.when(() -> SecurityUtils.hashPassword("wrong")).thenReturn("hashedWrong");
        mockedUserRepository.when(() -> UserRepository.validateCredentials("test@test.com", "hashedWrong")).thenReturn(false);
        loginHandler.handle(mockExchange);

        mockedHttpUtils.verify(() -> HttpUtils.sendResponse(eq(mockExchange), eq(401), eq("application/json; charset=UTF-8"), eq("{\"success\": false, \"message\": \"Wrong email or password!\"}")));
    }

    @Test
    void testHandlePostSqlException() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/login"));
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        InputStream inputStream = new ByteArrayInputStream("email=test@test.com&password=pass".getBytes(StandardCharsets.UTF_8));
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        Map<String, String> parsedData = new HashMap<>();
        parsedData.put("email", "test@test.com");
        parsedData.put("password", "pass");
        mockedHttpUtils.when(() -> HttpUtils.parseFormData(anyString())).thenReturn(parsedData);
        mockedSecurityUtils.when(() -> SecurityUtils.hashPassword("pass")).thenReturn("hashedPass");
        mockedUserRepository.when(() -> UserRepository.validateCredentials("test@test.com", "hashedPass")).thenThrow(SQLException.class);
        loginHandler.handle(mockExchange);

        mockedHttpUtils.verify(() -> HttpUtils.sendResponse(eq(mockExchange), eq(500), eq("application/json; charset=UTF-8"), eq("{\"success\": false, \"message\": \"Server error!\"}")));
    }
}