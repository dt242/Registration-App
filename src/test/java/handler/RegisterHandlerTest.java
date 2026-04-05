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

class RegisterHandlerTest {

    private HttpExchange mockExchange;
    private Headers mockHeaders;
    private MockedStatic<HttpUtils> mockedHttpUtils;
    private MockedStatic<SessionManager> mockedSessionManager;
    private MockedStatic<UserRepository> mockedUserRepository;
    private MockedStatic<SecurityUtils> mockedSecurityUtils;
    private RegisterHandler registerHandler;

    @BeforeEach
    void setUp() {
        mockExchange = mock(HttpExchange.class);
        mockHeaders = mock(Headers.class);
        mockedHttpUtils = mockStatic(HttpUtils.class);
        mockedSessionManager = mockStatic(SessionManager.class);
        mockedUserRepository = mockStatic(UserRepository.class);
        mockedSecurityUtils = mockStatic(SecurityUtils.class);
        registerHandler = new RegisterHandler();
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
        when(mockExchange.getRequestURI()).thenReturn(new URI("/wrong-register"));
        registerHandler.handle(mockExchange);

        mockedHttpUtils.verify(() -> HttpUtils.sendErrorPage(eq(mockExchange), eq(404), anyString()));
    }

    @Test
    void testHandleAlreadyLoggedIn() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/register"));
        mockedHttpUtils.when(() -> HttpUtils.getCookieValue(mockExchange, "session_token")).thenReturn("valid_token");
        mockedSessionManager.when(() -> SessionManager.isSessionValid("valid_token")).thenReturn(true);
        registerHandler.handle(mockExchange);

        verify(mockHeaders).add("Location", "/profile");
        verify(mockExchange).sendResponseHeaders(302, -1);
        verify(mockExchange).close();
    }

    @Test
    void testHandleInvalidMethod() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/register"));
        when(mockExchange.getRequestMethod()).thenReturn("PUT");
        mockedHttpUtils.when(() -> HttpUtils.getCookieValue(mockExchange, "session_token")).thenReturn(null);
        registerHandler.handle(mockExchange);

        mockedHttpUtils.verify(() -> HttpUtils.sendErrorPage(eq(mockExchange), eq(405), eq("Method Not Allowed")));
    }

    @Test
    void testHandleGetSuccessful() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/register"));
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        mockedHttpUtils.when(() -> HttpUtils.getCookieValue(mockExchange, "session_token")).thenReturn(null);
        registerHandler.handle(mockExchange);

        mockedHttpUtils.verify(() -> HttpUtils.sendResponse(eq(mockExchange), eq(200), eq("text/html; charset=UTF-8"), anyString()));
    }

    @Test
    void testHandlePostEmptyFields() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/register"));
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        mockedHttpUtils.when(() -> HttpUtils.getCookieValue(mockExchange, "session_token")).thenReturn(null);
        InputStream inputStream = new ByteArrayInputStream("dummy".getBytes(StandardCharsets.UTF_8));
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        Map<String, String> parsedData = new HashMap<>();
        parsedData.put("firstName", "Ivan");
        parsedData.put("lastName", "");
        mockedHttpUtils.when(() -> HttpUtils.parseFormData(anyString())).thenReturn(parsedData);
        registerHandler.handle(mockExchange);

        mockedHttpUtils.verify(() -> HttpUtils.sendResponse(eq(mockExchange), eq(400), eq("application/json; charset=UTF-8"), contains("All fields are required!")));
    }

    @Test
    void testHandlePostInvalidEmail() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/register"));
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        InputStream inputStream = new ByteArrayInputStream("dummy".getBytes(StandardCharsets.UTF_8));
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        Map<String, String> parsedData = new HashMap<>();
        parsedData.put("firstName", "Ivan");
        parsedData.put("lastName", "Ivanov");
        parsedData.put("email", "invalid-email");
        parsedData.put("password", "123456");
        parsedData.put("captcha", "ABCDE");
        mockedHttpUtils.when(() -> HttpUtils.parseFormData(anyString())).thenReturn(parsedData);
        registerHandler.handle(mockExchange);

        mockedHttpUtils.verify(() -> HttpUtils.sendResponse(eq(mockExchange), eq(400), eq("application/json; charset=UTF-8"), contains("valid email")));
    }

    @Test
    void testHandlePostShortPassword() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/register"));
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        InputStream inputStream = new ByteArrayInputStream("dummy".getBytes(StandardCharsets.UTF_8));
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        Map<String, String> parsedData = new HashMap<>();
        parsedData.put("firstName", "Ivan");
        parsedData.put("lastName", "Ivanov");
        parsedData.put("email", "test@test.com");
        parsedData.put("password", "123");
        parsedData.put("captcha", "ABCDE");
        mockedHttpUtils.when(() -> HttpUtils.parseFormData(anyString())).thenReturn(parsedData);
        registerHandler.handle(mockExchange);

        mockedHttpUtils.verify(() -> HttpUtils.sendResponse(eq(mockExchange), eq(400), eq("application/json; charset=UTF-8"), contains("at least 6 characters")));
    }

    @Test
    void testHandlePostWrongCaptcha() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/register"));
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        mockedHttpUtils.when(() -> HttpUtils.getCookieValue(mockExchange, "captcha_id")).thenReturn("captcha_123");
        InputStream inputStream = new ByteArrayInputStream("dummy".getBytes(StandardCharsets.UTF_8));
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        Map<String, String> parsedData = new HashMap<>();
        parsedData.put("firstName", "Ivan");
        parsedData.put("lastName", "Ivanov");
        parsedData.put("email", "test@test.com");
        parsedData.put("password", "123456");
        parsedData.put("captcha", "WRONG");
        mockedHttpUtils.when(() -> HttpUtils.parseFormData(anyString())).thenReturn(parsedData);
        mockedSessionManager.when(() -> SessionManager.validateAndConsumeCaptcha("captcha_123", "WRONG")).thenReturn(false);
        registerHandler.handle(mockExchange);

        mockedHttpUtils.verify(() -> HttpUtils.sendResponse(eq(mockExchange), eq(400), eq("application/json; charset=UTF-8"), contains("Wrong text")));
    }

    @Test
    void testHandlePostEmailExists() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/register"));
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        mockedHttpUtils.when(() -> HttpUtils.getCookieValue(mockExchange, "captcha_id")).thenReturn("captcha_123");
        InputStream inputStream = new ByteArrayInputStream("dummy".getBytes(StandardCharsets.UTF_8));
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        Map<String, String> parsedData = new HashMap<>();
        parsedData.put("firstName", "Ivan");
        parsedData.put("lastName", "Ivanov");
        parsedData.put("email", "test@test.com");
        parsedData.put("password", "123456");
        parsedData.put("captcha", "ABCDE");
        mockedHttpUtils.when(() -> HttpUtils.parseFormData(anyString())).thenReturn(parsedData);
        mockedSessionManager.when(() -> SessionManager.validateAndConsumeCaptcha("captcha_123", "ABCDE")).thenReturn(true);
        mockedUserRepository.when(() -> UserRepository.emailExists("test@test.com")).thenReturn(true);
        registerHandler.handle(mockExchange);

        mockedHttpUtils.verify(() -> HttpUtils.sendResponse(eq(mockExchange), eq(400), eq("application/json; charset=UTF-8"), contains("already taken")));
    }

    @Test
    void testHandlePostSuccess() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/register"));
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        mockedHttpUtils.when(() -> HttpUtils.getCookieValue(mockExchange, "captcha_id")).thenReturn("captcha_123");
        InputStream inputStream = new ByteArrayInputStream("dummy".getBytes(StandardCharsets.UTF_8));
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        Map<String, String> parsedData = new HashMap<>();
        parsedData.put("firstName", "Ivan");
        parsedData.put("lastName", "Ivanov");
        parsedData.put("email", "test@test.com");
        parsedData.put("password", "123456");
        parsedData.put("captcha", "ABCDE");
        mockedHttpUtils.when(() -> HttpUtils.parseFormData(anyString())).thenReturn(parsedData);
        mockedSessionManager.when(() -> SessionManager.validateAndConsumeCaptcha("captcha_123", "ABCDE")).thenReturn(true);
        mockedUserRepository.when(() -> UserRepository.emailExists("test@test.com")).thenReturn(false);
        mockedSecurityUtils.when(() -> SecurityUtils.hashPassword("123456")).thenReturn("hashedPass");
        registerHandler.handle(mockExchange);

        mockedUserRepository.verify(() -> UserRepository.createUser("Ivan", "Ivanov", "test@test.com", "hashedPass"));
        mockedHttpUtils.verify(() -> HttpUtils.sendResponse(eq(mockExchange), eq(200), eq("application/json; charset=UTF-8"), contains("Registration was successful")));
    }

    @Test
    void testHandlePostSqlException() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/register"));
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        mockedHttpUtils.when(() -> HttpUtils.getCookieValue(mockExchange, "captcha_id")).thenReturn("captcha_123");
        InputStream inputStream = new ByteArrayInputStream("dummy".getBytes(StandardCharsets.UTF_8));
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        Map<String, String> parsedData = new HashMap<>();
        parsedData.put("firstName", "Ivan");
        parsedData.put("lastName", "Ivanov");
        parsedData.put("email", "test@test.com");
        parsedData.put("password", "123456");
        parsedData.put("captcha", "ABCDE");
        mockedHttpUtils.when(() -> HttpUtils.parseFormData(anyString())).thenReturn(parsedData);
        mockedSessionManager.when(() -> SessionManager.validateAndConsumeCaptcha("captcha_123", "ABCDE")).thenReturn(true);
        mockedUserRepository.when(() -> UserRepository.emailExists("test@test.com")).thenThrow(SQLException.class);
        registerHandler.handle(mockExchange);

        mockedHttpUtils.verify(() -> HttpUtils.sendResponse(eq(mockExchange), eq(500), eq("application/json; charset=UTF-8"), contains("Server error")));
    }
}