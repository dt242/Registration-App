package handler;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
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

class RootHandlerTest {

    private HttpExchange mockExchange;
    private MockedStatic<HttpUtils> mockedHttpUtils;
    private RootHandler rootHandler;
    private Headers mockHeaders;

    @BeforeEach
    void setUp() {
        mockExchange = mock(HttpExchange.class);
        mockedHttpUtils = mockStatic(HttpUtils.class);
        rootHandler = new RootHandler();
        mockHeaders = mock(Headers.class);
    }

    @AfterEach
    void tearDown() {
        mockedHttpUtils.close();
    }

    @Test
    void testHandleRootPathRedirect() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/"));
        when(mockExchange.getResponseHeaders()).thenReturn(mockHeaders);
        rootHandler.handle(mockExchange);

        verify(mockHeaders).add("Location", "/login");
        verify(mockExchange).sendResponseHeaders(302, -1);
        verify(mockExchange).close();
        mockedHttpUtils.verifyNoInteractions();
    }

    @Test
    void testHandleUnknownPathError() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/unknown"));
        rootHandler.handle(mockExchange);

        mockedHttpUtils.verify(() -> HttpUtils.sendErrorPage(eq(mockExchange), eq(404), eq("The page you are looking for has been moved or does not exist.")));
        verify(mockExchange, never()).sendResponseHeaders(anyInt(), anyLong());
    }
}