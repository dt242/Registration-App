package model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserTest {

    @Test
    void testUserConstructorAndGetters() {
        User user = new User("Daniel", "Petrov", "dani@test.com");

        assertEquals("Daniel", user.getFirstName());
        assertEquals("Petrov", user.getLastName());
        assertEquals("dani@test.com", user.getEmail());
    }
}