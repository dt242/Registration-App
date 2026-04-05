package core;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static final Map<String, String> activeSessions = new ConcurrentHashMap<>();
    private static final Map<String, String> activeCaptchas = new ConcurrentHashMap<>();

    public static String createSession(String email) {
        String sessionToken = UUID.randomUUID().toString();
        activeSessions.put(sessionToken, email);
        return sessionToken;
    }

    public static String getEmailByToken(String sessionToken) {
        if (sessionToken == null) return null;
        return activeSessions.get(sessionToken);
    }

    public static boolean isSessionValid(String sessionToken) {
        return sessionToken != null && activeSessions.containsKey(sessionToken);
    }

    public static void invalidateSession(String sessionToken) {
        if (sessionToken != null) {
            activeSessions.remove(sessionToken);
        }
    }

    public static String saveCaptcha(String captchaText) {
        String captchaId = UUID.randomUUID().toString();
        activeCaptchas.put(captchaId, captchaText);
        return captchaId;
    }

    public static boolean validateAndConsumeCaptcha(String captchaId, String userCaptchaText) {
        if (captchaId == null || userCaptchaText == null) {
            return false;
        }
        String realCaptchaText = activeCaptchas.get(captchaId);
        activeCaptchas.remove(captchaId);
        return realCaptchaText != null && realCaptchaText.equalsIgnoreCase(userCaptchaText);
    }
}