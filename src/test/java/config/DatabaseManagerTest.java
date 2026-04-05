package config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DatabaseManagerTest {

    private MockedStatic<DriverManager> mockedDriverManager;
    private Connection mockConnection;

    @BeforeEach
    void setUp() {
        mockConnection = mock(Connection.class);
        mockedDriverManager = mockStatic(DriverManager.class);
    }

    @AfterEach
    void tearDown() {
        mockedDriverManager.close();
    }

    @Test
    void testGetConnectionSuccess() throws SQLException {
        mockedDriverManager.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString())).thenReturn(mockConnection);
        Connection result = DatabaseManager.getConnection();

        assertEquals(mockConnection, result);
        mockedDriverManager.verify(() -> DriverManager.getConnection(anyString(), anyString(), anyString()));
    }

    @Test
    void testGetConnectionThrowsSQLException() {
        mockedDriverManager.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString())).thenThrow(SQLException.class);

        assertThrows(SQLException.class, DatabaseManager::getConnection);
    }
}