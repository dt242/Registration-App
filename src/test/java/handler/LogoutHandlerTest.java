package handler;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import core.SessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import util.HttpUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LogoutHandlerTest {

    private HttpExchange mockExchange;
    private Headers mockHeaders;
    private MockedStatic<HttpUtils> mockedHttpUtils;
    private MockedStatic<SessionManager> mockedSessionManager;
    private LogoutHandler logoutHandler;

    @BeforeEach
    void setUp() {
        mockExchange = mock(HttpExchange.class);
        mockHeaders = mock(Headers.class);
        mockedHttpUtils = mockStatic(HttpUtils.class);
        mockedSessionManager = mockStatic(SessionManager.class);
        logoutHandler = new LogoutHandler();
    }

    @AfterEach
    void tearDown() {
        mockedHttpUtils.close();
        mockedSessionManager.close();
    }

    @Test
    void testHandleWrongPath() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/invalid-logout"));
        logoutHandler.handle(mockExchange);

        mockedHttpUtils.verify(() -> HttpUtils.sendErrorPage(eq(mockExchange), eq(404), eq("The page you are looking for has been moved or does not exist.")));
        mockedSessionManager.verifyNoInteractions();
    }

    @Test
    void testHandleSuccessfulLogout() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/logout"));
        when(mockExchange.getResponseHeaders()).thenReturn(mockHeaders);
        mockedHttpUtils.when(() -> HttpUtils.getCookieValue(mockExchange, "session_token")).thenReturn("dummy_token_123");
        logoutHandler.handle(mockExchange);

        mockedSessionManager.verify(() -> SessionManager.invalidateSession("dummy_token_123"));
        verify(mockHeaders).add("Set-Cookie", "session_token=; HttpOnly; Path=/; Max-Age=0");
        verify(mockHeaders).add("Location", "/login");
        verify(mockExchange).sendResponseHeaders(302, -1);
        verify(mockExchange).close();
    }

    @Test
    void testHandleLogoutWithoutToken() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/logout"));
        when(mockExchange.getResponseHeaders()).thenReturn(mockHeaders);
        mockedHttpUtils.when(() -> HttpUtils.getCookieValue(mockExchange, "session_token")).thenReturn(null);
        logoutHandler.handle(mockExchange);

        mockedSessionManager.verify(() -> SessionManager.invalidateSession(null));
        verify(mockHeaders).add("Set-Cookie", "session_token=; HttpOnly; Path=/; Max-Age=0");
        verify(mockHeaders).add("Location", "/login");
        verify(mockExchange).sendResponseHeaders(302, -1);
        verify(mockExchange).close();
    }
}