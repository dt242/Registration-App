package util;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class HttpUtilsTest {

    @Test
    void testParseFormData() {
        String formData = "firstName=Ivan&lastName=Ivanov%20Petrov";
        Map<String, String> result = HttpUtils.parseFormData(formData);

        assertEquals(2, result.size());
        assertEquals("Ivan", result.get("firstName"));
        assertEquals("Ivanov Petrov", result.get("lastName"));
    }

    @Test
    void testParseFormDataEmpty() {
        Map<String, String> result = HttpUtils.parseFormData("");

        assertTrue(result.isEmpty());
    }

    @Test
    void testGetCookieValueFound() {
        HttpExchange exchange = mock(HttpExchange.class);
        Headers headers = new Headers();
        headers.put("Cookie", List.of("other=123; session_token=testToken123; HttpOnly"));
        when(exchange.getRequestHeaders()).thenReturn(headers);
        String result = HttpUtils.getCookieValue(exchange, "session_token");

        assertEquals("testToken123", result);
    }

    @Test
    void testGetCookieValueNotFound() {
        HttpExchange exchange = mock(HttpExchange.class);
        Headers headers = new Headers();
        headers.put("Cookie", List.of("other_cookie=123"));
        when(exchange.getRequestHeaders()).thenReturn(headers);
        String result = HttpUtils.getCookieValue(exchange, "session_token");

        assertNull(result);
    }

    @Test
    void testGetCookieValueNoHeaders() {
        HttpExchange exchange = mock(HttpExchange.class);
        when(exchange.getRequestHeaders()).thenReturn(new Headers());
        String result = HttpUtils.getCookieValue(exchange, "session_token");

        assertNull(result);
    }

    @Test
    void testSendResponse() throws IOException {
        HttpExchange exchange = mock(HttpExchange.class);
        Headers headers = mock(Headers.class);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(exchange.getResponseHeaders()).thenReturn(headers);
        when(exchange.getResponseBody()).thenReturn(outputStream);
        HttpUtils.sendResponse(exchange, 200, "application/json", "{\"status\":\"ok\"}");

        verify(headers).set("Content-Type", "application/json");
        verify(headers).set("Cache-Control", "no-cache, no-store, must-revalidate");
        verify(headers).set("Pragma", "no-cache");
        verify(headers).set("Expires", "0");
        verify(exchange).sendResponseHeaders(200, 15);
        assertEquals("{\"status\":\"ok\"}", outputStream.toString());
    }

    @Test
    void testSendResponsePlainNoCache() throws IOException {
        HttpExchange exchange = mock(HttpExchange.class);
        Headers headers = mock(Headers.class);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(exchange.getResponseHeaders()).thenReturn(headers);
        when(exchange.getResponseBody()).thenReturn(outputStream);
        HttpUtils.sendResponse(exchange, 404, "text/plain", "Not Found");

        verify(headers).set("Content-Type", "text/plain");
        verify(headers, never()).set("Cache-Control", "no-cache, no-store, must-revalidate");
        verify(exchange).sendResponseHeaders(404, 9);
        assertEquals("Not Found", outputStream.toString());
    }

    @Test
    void testSendErrorPage() throws IOException {
        HttpExchange exchange = mock(HttpExchange.class);
        Headers headers = mock(Headers.class);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(exchange.getResponseHeaders()).thenReturn(headers);
        when(exchange.getResponseBody()).thenReturn(outputStream);
        HttpUtils.sendErrorPage(exchange, 500, "Server Error");

        verify(headers).set("Content-Type", "text/html; charset=UTF-8");
        verify(headers).set("Cache-Control", "no-cache, no-store, must-revalidate");
        verify(headers).set("Pragma", "no-cache");
        verify(headers).set("Expires", "0");
        verify(exchange).sendResponseHeaders(eq(500), anyLong());

        String output = outputStream.toString();
        assertTrue(output.contains("500"));
        assertTrue(output.contains("Server Error"));
    }
}