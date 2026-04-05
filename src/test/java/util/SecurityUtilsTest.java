package util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest {

    @Test
    void testHashPassword_Consistency() {
        String password = "mySuperSecretPassword123!";
        String hash1 = SecurityUtils.hashPassword(password);
        String hash2 = SecurityUtils.hashPassword(password);

        assertNotNull(hash1);
        assertEquals(hash1, hash2);
    }

    @Test
    void testHashPassword_Uniqueness() {
        String passwordOne = "password123";
        String passwordTwo = "Password123";
        String hash1 = SecurityUtils.hashPassword(passwordOne);
        String hash2 = SecurityUtils.hashPassword(passwordTwo);

        assertNotEquals(hash1, hash2);
    }

    @Test
    void testHashPassword_EmptyString() {
        String hash = SecurityUtils.hashPassword("");

        assertNotNull(hash);
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hash);
    }

    @Test
    void testHashPassword_NullInput() {
        assertThrows(RuntimeException.class, () -> {
            SecurityUtils.hashPassword(null);
        });
    }
}