package handler;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import core.SessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import util.HttpUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CaptchaHandlerTest {

    private HttpExchange mockExchange;
    private Headers mockHeaders;
    private MockedStatic<HttpUtils> mockedHttpUtils;
    private MockedStatic<SessionManager> mockedSessionManager;
    private CaptchaHandler captchaHandler;

    @BeforeEach
    void setUp() {
        mockExchange = mock(HttpExchange.class);
        mockHeaders = mock(Headers.class);
        mockedHttpUtils = mockStatic(HttpUtils.class);
        mockedSessionManager = mockStatic(SessionManager.class);
        captchaHandler = new CaptchaHandler();
        when(mockExchange.getResponseHeaders()).thenReturn(mockHeaders);
    }

    @AfterEach
    void tearDown() {
        mockedHttpUtils.close();
        mockedSessionManager.close();
    }

    @Test
    void testHandleWrongPath() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/invalid-captcha-path"));
        captchaHandler.handle(mockExchange);

        mockedHttpUtils.verify(() -> HttpUtils.sendErrorPage(eq(mockExchange), eq(404), eq("The page you are looking for has been moved or does not exist.")));
    }

    @Test
    void testHandleWrongMethod() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/captcha"));
        when(mockExchange.getRequestMethod()).thenReturn("POST");
        captchaHandler.handle(mockExchange);

        mockedHttpUtils.verify(() -> HttpUtils.sendErrorPage(eq(mockExchange), eq(405), eq("Method Not Allowed")));
    }

    @Test
    void testHandleSuccessfulCaptchaGeneration() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/captcha"));
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(mockExchange.getResponseBody()).thenReturn(outputStream);
        mockedSessionManager.when(() -> SessionManager.saveCaptcha(anyString())).thenReturn("mocked-uuid-123");
        captchaHandler.handle(mockExchange);

        mockedSessionManager.verify(() -> SessionManager.saveCaptcha(anyString()));
        verify(mockHeaders).add("Set-Cookie", "captcha_id=mocked-uuid-123; HttpOnly; Path=/");
        verify(mockHeaders).set("Content-Type", "image/png");
        verify(mockHeaders).set("Cache-Control", "no-cache, no-store, must-revalidate");
        verify(mockHeaders).set("Pragma", "no-cache");
        verify(mockHeaders).set("Expires", "0");
        verify(mockExchange).sendResponseHeaders(200, 0);
        verify(mockExchange).close();
        assertTrue(outputStream.toByteArray().length > 0);
    }
}