package core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionManagerTest {

    @Test
    void testCreateAndGetSession() {
        String email = "test@example.com";
        String token = SessionManager.createSession(email);

        assertNotNull(token);
        assertEquals(email, SessionManager.getEmailByToken(token));
    }

    @Test
    void testGetEmailByNullToken() {
        assertNull(SessionManager.getEmailByToken(null));
    }

    @Test
    void testGetEmailByInvalidToken() {
        assertNull(SessionManager.getEmailByToken("invalid-token-123"));
    }

    @Test
    void testIsSessionValid() {
        String token = SessionManager.createSession("valid@example.com");

        assertTrue(SessionManager.isSessionValid(token));
        assertFalse(SessionManager.isSessionValid("invalid-token-123"));
    }

    @Test
    void testInvalidateSession() {
        String token = SessionManager.createSession("invalidate@example.com");
        assertTrue(SessionManager.isSessionValid(token));

        SessionManager.invalidateSession(token);
        assertFalse(SessionManager.isSessionValid(token));
        assertNull(SessionManager.getEmailByToken(token));
    }

    @Test
    void testInvalidateNullSession() {
        assertDoesNotThrow(() -> SessionManager.invalidateSession(null));
    }

    @Test
    void testSaveAndValidateCaptchaExactMatch() {
        String captchaId = SessionManager.saveCaptcha("A1B2C");

        assertNotNull(captchaId);
        assertTrue(SessionManager.validateAndConsumeCaptcha(captchaId, "A1B2C"));
    }

    @Test
    void testSaveAndValidateCaptchaCaseInsensitive() {
        String captchaId = SessionManager.saveCaptcha("XyZ99");

        assertTrue(SessionManager.validateAndConsumeCaptcha(captchaId, "xyz99"));
    }

    @Test
    void testValidateCaptchaIsConsumedOnSuccess() {
        String captchaId = SessionManager.saveCaptcha("QWERTY");

        assertTrue(SessionManager.validateAndConsumeCaptcha(captchaId, "QWERTY"));
        assertFalse(SessionManager.validateAndConsumeCaptcha(captchaId, "QWERTY"));
    }

    @Test
    void testValidateCaptchaIsConsumedOnFailure() {
        String captchaId = SessionManager.saveCaptcha("ASDFG");

        assertFalse(SessionManager.validateAndConsumeCaptcha(captchaId, "WRONG"));
        assertFalse(SessionManager.validateAndConsumeCaptcha(captchaId, "ASDFG"));
    }

    @Test
    void testValidateAndConsumeCaptchaNullParams() {
        assertFalse(SessionManager.validateAndConsumeCaptcha(null, "TEXT"));
        assertFalse(SessionManager.validateAndConsumeCaptcha("some-id", null));
        assertFalse(SessionManager.validateAndConsumeCaptcha(null, null));
    }
}