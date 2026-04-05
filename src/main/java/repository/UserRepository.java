package repository;

import config.DatabaseManager;
import model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRepository {
    public static boolean emailExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, email);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    public static void createUser(String firstName, String lastName, String email, String hashedPassword) throws SQLException {
        String sql = "INSERT INTO users (first_name, last_name, email, password_hash) VALUES (?, ?, ?, ?)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, firstName);
            preparedStatement.setString(2, lastName);
            preparedStatement.setString(3, email);
            preparedStatement.setString(4, hashedPassword);
            preparedStatement.executeUpdate();
        }
    }

    public static boolean validateCredentials(String email, String hashedPassword) throws SQLException {
        String sql = "SELECT first_name FROM users WHERE email = ? AND password_hash = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, email);
            preparedStatement.setString(2, hashedPassword);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public static User getUserByEmail(String email) throws SQLException {
        String sql = "SELECT first_name, last_name FROM users WHERE email = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, email);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return new User(resultSet.getString("first_name"), resultSet.getString("last_name"), email);
                }
            }
        }
        return null;
    }

    public static void updateProfile(String email, String newFirstName, String newLastName, String newHashedPassword) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection()) {
            if (newHashedPassword != null && !newHashedPassword.trim().isEmpty()) {
                String sql = "UPDATE users SET first_name = ?, last_name = ?, password_hash = ? WHERE email = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                    preparedStatement.setString(1, newFirstName);
                    preparedStatement.setString(2, newLastName);
                    preparedStatement.setString(3, newHashedPassword);
                    preparedStatement.setString(4, email);
                    preparedStatement.executeUpdate();
                }
            } else {
                String sql = "UPDATE users SET first_name = ?, last_name = ? WHERE email = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                    preparedStatement.setString(1, newFirstName);
                    preparedStatement.setString(2, newLastName);
                    preparedStatement.setString(3, email);
                    preparedStatement.executeUpdate();
                }
            }
        }
    }
}