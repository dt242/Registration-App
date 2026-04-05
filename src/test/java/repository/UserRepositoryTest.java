package repository;

import config.DatabaseManager;
import model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserRepositoryTest {

    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private ResultSet mockResultSet;
    private MockedStatic<DatabaseManager> mockedDatabaseManager;

    @BeforeEach
    void setUp() throws SQLException {
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        mockedDatabaseManager = mockStatic(DatabaseManager.class);
        mockedDatabaseManager.when(DatabaseManager::getConnection).thenReturn(mockConnection);
    }

    @AfterEach
    void tearDown() {
        mockedDatabaseManager.close();
    }

    @Test
    void testEmailExistsTrue() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(1);
        boolean result = UserRepository.emailExists("test@test.com");

        assertTrue(result);
        verify(mockPreparedStatement).setString(1, "test@test.com");
    }

    @Test
    void testEmailExistsFalse() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(0);
        boolean result = UserRepository.emailExists("test@test.com");

        assertFalse(result);
    }

    @Test
    void testEmailExistsNoResults() throws SQLException {
        when(mockResultSet.next()).thenReturn(false);
        boolean result = UserRepository.emailExists("test@test.com");

        assertFalse(result);
    }

    @Test
    void testCreateUser() throws SQLException {
        UserRepository.createUser("Ivan", "Ivanov", "ivan@test.com", "hashedPass");

        verify(mockPreparedStatement).setString(1, "Ivan");
        verify(mockPreparedStatement).setString(2, "Ivanov");
        verify(mockPreparedStatement).setString(3, "ivan@test.com");
        verify(mockPreparedStatement).setString(4, "hashedPass");
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    void testValidateCredentialsTrue() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        boolean result = UserRepository.validateCredentials("ivan@test.com", "hashedPass");

        assertTrue(result);
        verify(mockPreparedStatement).setString(1, "ivan@test.com");
        verify(mockPreparedStatement).setString(2, "hashedPass");
    }

    @Test
    void testValidateCredentialsFalse() throws SQLException {
        when(mockResultSet.next()).thenReturn(false);
        boolean result = UserRepository.validateCredentials("ivan@test.com", "wrongPass");

        assertFalse(result);
    }

    @Test
    void testGetUserByEmailFound() throws SQLException {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("first_name")).thenReturn("Ivan");
        when(mockResultSet.getString("last_name")).thenReturn("Ivanov");
        User result = UserRepository.getUserByEmail("ivan@test.com");

        assertNotNull(result);
        assertEquals("Ivan", result.getFirstName());
        assertEquals("Ivanov", result.getLastName());
        assertEquals("ivan@test.com", result.getEmail());
        verify(mockPreparedStatement).setString(1, "ivan@test.com");
    }

    @Test
    void testGetUserByEmailNotFound() throws SQLException {
        when(mockResultSet.next()).thenReturn(false);
        User result = UserRepository.getUserByEmail("notfound@test.com");

        assertNull(result);
    }

    @Test
    void testUpdateProfileWithPassword() throws SQLException {
        UserRepository.updateProfile("ivan@test.com", "Petar", "Petrov", "newHash");

        verify(mockPreparedStatement).setString(1, "Petar");
        verify(mockPreparedStatement).setString(2, "Petrov");
        verify(mockPreparedStatement).setString(3, "newHash");
        verify(mockPreparedStatement).setString(4, "ivan@test.com");
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    void testUpdateProfileWithoutPasswordNull() throws SQLException {
        UserRepository.updateProfile("ivan@test.com", "Petar", "Petrov", null);

        verify(mockPreparedStatement).setString(1, "Petar");
        verify(mockPreparedStatement).setString(2, "Petrov");
        verify(mockPreparedStatement).setString(3, "ivan@test.com");
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    void testUpdateProfileWithoutPasswordEmpty() throws SQLException {
        UserRepository.updateProfile("ivan@test.com", "Petar", "Petrov", "   ");

        verify(mockPreparedStatement).setString(1, "Petar");
        verify(mockPreparedStatement).setString(2, "Petrov");
        verify(mockPreparedStatement).setString(3, "ivan@test.com");
        verify(mockPreparedStatement).executeUpdate();
    }
}