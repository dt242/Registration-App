package config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DatabaseManagerTest {

    private MockedStatic<DriverManager> mockedDriverManager;
    private Connection mockConnection;
    private Statement mockStatement;
    private PreparedStatement mockPreparedStatement;
    private ResultSet mockResultSet;

    @BeforeEach
    void setUp() {
        mockConnection = mock(Connection.class);
        mockStatement = mock(Statement.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);
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

    @Test
    void testInitDatabase_Success() throws SQLException {
        mockedDriverManager.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString())).thenReturn(mockConnection);
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(0);
        DatabaseManager.initDatabase();

        verify(mockStatement).executeUpdate(contains("CREATE DATABASE IF NOT EXISTS"));
        verify(mockStatement).executeUpdate(contains("CREATE TABLE IF NOT EXISTS users"));
        verify(mockPreparedStatement).executeUpdate();
    }
}