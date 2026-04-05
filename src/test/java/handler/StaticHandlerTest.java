package handler;

import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import util.HttpUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class StaticHandlerTest {

    private HttpExchange mockExchange;
    private MockedStatic<HttpUtils> mockedHttpUtils;
    private StaticHandler staticHandler;

    @BeforeEach
    void setUp() {
        mockExchange = mock(HttpExchange.class);
        mockedHttpUtils = mockStatic(HttpUtils.class);
        staticHandler = new StaticHandler();
    }

    @AfterEach
    void tearDown() {
        mockedHttpUtils.close();
    }

    @Test
    void testHandleWrongPath() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/wrong-style.css"));
        staticHandler.handle(mockExchange);

        mockedHttpUtils.verify(() -> HttpUtils.sendErrorPage(mockExchange, 404, "Not Found"));
    }

    @Test
    void testHandleCorrectPath() throws IOException, URISyntaxException {
        when(mockExchange.getRequestURI()).thenReturn(new URI("/style.css"));
        staticHandler.handle(mockExchange);

        mockedHttpUtils.verify(() -> HttpUtils.sendResponse(eq(mockExchange), eq(200), eq("text/css; charset=UTF-8"), anyString()));
    }
}