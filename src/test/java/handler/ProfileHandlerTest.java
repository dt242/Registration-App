package handler;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import core.SessionManager;
import model.User;
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

class ProfileHandlerTest {

    private HttpExchange mockExchange;
    private Headers mockHeaders;
    private MockedStatic<HttpUtils> mockedHttpUtils;
    private MockedStatic<SessionManager> mockedSessionManager;
    private MockedStatic<UserRepository> mockedUserRepository;
    private MockedStatic<SecurityUtils> mockedSecurityUtils;
    private ProfileHandler profileHandler;

    @BeforeEach
    void setUp() {
        mockExchange = mock(HttpExchange.class);
        mockHeaders = mock(Headers.class);
        mockedHttpUtils = mockStatic(HttpUtils.class);
        mockedSessionManager = mockStatic(SessionManager.class);
        mockedUserRepository = mockStatic(UserRepository.class);
        mockedSecurityUtils = mockStatic(SecurityUtils.class);
        profileHandler = new ProfileHandler();
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
        when(mockExchange.getRequestURI()).thenReturn(new URI("/wrongprofile"));
        profileHandler.handle(mockExchange);

        mockedHttpUtils.verify(() -> HttpUtils.sendErrorPage(eq(mockExchange), eq(404), anyString()));
    }

    @Test
    void testHandleUnauthorizedUserRedirects() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/profile"));
        mockedHttpUtils.when(() -> HttpUtils.getCookieValue(mockExchange, "session_token")).thenReturn("invalid_token");
        mockedSessionManager.when(() -> SessionManager.getEmailByToken("invalid_token")).thenReturn(null);
        profileHandler.handle(mockExchange);

        verify(mockHeaders).add("Location", "/login");
        verify(mockExchange).sendResponseHeaders(302, -1);
        verify(mockExchange).close();
    }

    @Test
    void testHandleInvalidMethod() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/profile"));
        when(mockExchange.getRequestMethod()).thenReturn("PUT");
        mockedSessionManager.when(() -> SessionManager.getEmailByToken(any())).thenReturn("user@test.com");
        profileHandler.handle(mockExchange);

        mockedHttpUtils.verify(() -> HttpUtils.sendErrorPage(eq(mockExchange), eq(405), eq("Method Not Allowed")));
    }

    @Test
    void testHandleGetSuccessful() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/profile"));
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        mockedSessionManager.when(() -> SessionManager.getEmailByToken(any())).thenReturn("user@test.com");
        User mockUser = new User("Daniel", "Petrov", "user@test.com");
        mockedUserRepository.when(() -> UserRepository.getUserByEmail("user@test.com")).thenReturn(mockUser);
        profileHandler.handle(mockExchange);

        mockedHttpUtils.verify(() -> HttpUtils.sendResponse(eq(mockExchange), eq(200), eq("text/html; charset=UTF-8"), anyString()));
    }

    @Test
    void testHandleGetUserNotFound() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/profile"));
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        mockedSessionManager.when(() -> SessionManager.getEmailByToken(any())).thenReturn("user@test.com");
        mockedUserRepository.when(() -> UserRepository.getUserByEmail("user@test.com")).thenReturn(null);
        profileHandler.handle(mockExchange);

        mockedHttpUtils.verify(() -> HttpUtils.sendErrorPage(eq(mockExchange), eq(404), eq("User not found in database!")));
    }

    @Test
    void testHandleGetSqlException() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/profile"));
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        mockedSessionManager.when(() -> SessionManager.getEmailByToken(any())).thenReturn("user@test.com");
        mockedUserRepository.when(() -> UserRepository.getUserByEmail("user@test.com")).thenThrow(SQLException.class);
        profileHandler.handle(mockExchange);

        mockedHttpUtils.verify(() -> HttpUtils.sendErrorPage(eq(mockExchange), eq(500), eq("Database error!")));
    }

    @Test
    void testHandlePostEmptyNames() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/profile"));
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        mockedSessionManager.when(() -> SessionManager.getEmailByToken(any())).thenReturn("user@test.com");
        InputStream inputStream = new ByteArrayInputStream("firstName=&lastName=".getBytes(StandardCharsets.UTF_8));
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        Map<String, String> parsedData = new HashMap<>();
        parsedData.put("firstName", "");
        parsedData.put("lastName", null);
        mockedHttpUtils.when(() -> HttpUtils.parseFormData(anyString())).thenReturn(parsedData);
        profileHandler.handle(mockExchange);

        mockedHttpUtils.verify(() -> HttpUtils.sendResponse(eq(mockExchange), eq(400), eq("application/json; charset=UTF-8"), anyString()));
    }

    @Test
    void testHandlePostShortPassword() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/profile"));
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        mockedSessionManager.when(() -> SessionManager.getEmailByToken(any())).thenReturn("user@test.com");
        InputStream inputStream = new ByteArrayInputStream("dummy".getBytes(StandardCharsets.UTF_8));
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        Map<String, String> parsedData = new HashMap<>();
        parsedData.put("firstName", "Ivan");
        parsedData.put("lastName", "Ivanov");
        parsedData.put("password", "12345");
        mockedHttpUtils.when(() -> HttpUtils.parseFormData(anyString())).thenReturn(parsedData);
        profileHandler.handle(mockExchange);

        mockedHttpUtils.verify(() -> HttpUtils.sendResponse(eq(mockExchange), eq(400), eq("application/json; charset=UTF-8"), anyString()));
    }

    @Test
    void testHandlePostSuccessWithPassword() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/profile"));
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        mockedSessionManager.when(() -> SessionManager.getEmailByToken(any())).thenReturn("user@test.com");
        InputStream inputStream = new ByteArrayInputStream("dummy".getBytes(StandardCharsets.UTF_8));
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        Map<String, String> parsedData = new HashMap<>();
        parsedData.put("firstName", "Ivan");
        parsedData.put("lastName", "Ivanov");
        parsedData.put("password", "securePass");
        mockedHttpUtils.when(() -> HttpUtils.parseFormData(anyString())).thenReturn(parsedData);
        mockedSecurityUtils.when(() -> SecurityUtils.hashPassword("securePass")).thenReturn("hashedPass");
        profileHandler.handle(mockExchange);

        mockedUserRepository.verify(() -> UserRepository.updateProfile("user@test.com", "Ivan", "Ivanov", "hashedPass"));
        mockedHttpUtils.verify(() -> HttpUtils.sendResponse(eq(mockExchange), eq(200), eq("application/json; charset=UTF-8"), eq("{\"success\": true}")));
    }

    @Test
    void testHandlePostSuccessWithoutPassword() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/profile"));
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        mockedSessionManager.when(() -> SessionManager.getEmailByToken(any())).thenReturn("user@test.com");
        InputStream inputStream = new ByteArrayInputStream("dummy".getBytes(StandardCharsets.UTF_8));
        when(mockExchange.getRequestBody()).thenReturn(inputStream);
        Map<String, String> parsedData = new HashMap<>();
        parsedData.put("firstName", "Ivan");
        parsedData.put("lastName", "Ivanov");
        parsedData.put("password", "");
        mockedHttpUtils.when(() -> HttpUtils.parseFormData(anyString())).thenReturn(parsedData);
        profileHandler.handle(mockExchange);

        mockedUserRepository.verify(() -> UserRepository.updateProfile("user@test.com", "Ivan", "Ivanov", null));
        mockedHttpUtils.verify(() -> HttpUtils.sendResponse(eq(mockExchange), eq(200), eq("application/json; charset=UTF-8"), eq("{\"success\": true}")));
    }
}