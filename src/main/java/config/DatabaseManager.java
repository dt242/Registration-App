package config;

import util.SecurityUtils;

import java.sql.*;

public class DatabaseManager {
    private static final String BASE_URL = "jdbc:mysql://localhost:3306/";
    private static final String DB_NAME = "registration_app_db";
    private static final String URL = BASE_URL + DB_NAME;
    private static final String USER = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "root";
    private static final String PASSWORD = System.getenv("DB_PASS") != null ? System.getenv("DB_PASS") : "Dani";

    public static void initDatabase() {
        try (Connection connection = DriverManager.getConnection(BASE_URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
            try (Connection dbConnection = getConnection();
                 Statement dbStatement = dbConnection.createStatement()) {
                String createTableSql = "CREATE TABLE IF NOT EXISTS users (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "first_name VARCHAR(50) NOT NULL, " +
                        "last_name VARCHAR(50) NOT NULL, " +
                        "email VARCHAR(100) NOT NULL UNIQUE, " +
                        "password_hash VARCHAR(255) NOT NULL" +
                        ")";
                dbStatement.executeUpdate(createTableSql);
                String testEmail = "test@test.com";
                String checkSql = "SELECT COUNT(*) FROM users WHERE email = '" + testEmail + "'";
                try (ResultSet resultSet = dbStatement.executeQuery(checkSql)) {
                    if (resultSet.next() && resultSet.getInt(1) == 0) {
                        String insertSql = "INSERT INTO users (first_name, last_name, email, password_hash) VALUES (?, ?, ?, ?)";
                        try (PreparedStatement preparedStatement = dbConnection.prepareStatement(insertSql)) {
                            preparedStatement.setString(1, "Test");
                            preparedStatement.setString(2, "User");
                            preparedStatement.setString(3, testEmail);
                            preparedStatement.setString(4, SecurityUtils.hashPassword("123456"));
                            preparedStatement.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}