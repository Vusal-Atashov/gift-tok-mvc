package domain.repository.impl;

import config.JdbcConnection;
import domain.entity.Users;
import domain.repository.UserRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserRepositoryImpl implements UserRepository {

    public UserRepositoryImpl() {
    }

    @Override
    public Users save(Users user) {
        String query = "INSERT INTO users (username, profile_name, picture_base64) " +
                "VALUES (?, ?, ?) " +
                "ON CONFLICT (username) " +
                "DO UPDATE SET profile_name = EXCLUDED.profile_name, picture_base64 = EXCLUDED.picture_base64 " +
                "RETURNING id";
        try (Connection conn = JdbcConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, user.getUserName());
            stmt.setString(2, user.getProfileName());
            stmt.setString(3, user.getPictureBase64());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    user.setId(rs.getLong("id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return user;
    }

    @Override
    public Users findById(Long id) {
        String query = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = JdbcConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Users(rs.getLong("id"), rs.getString("username"), rs.getString("profile_name"), rs.getString("picture_base64"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Users findByName(String name) {
        String query = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = JdbcConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Users(rs.getLong("id"), rs.getString("username"), rs.getString("profile_name"), rs.getString("picture_base64"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void delete(Users user) {
        String query = "DELETE FROM users WHERE id = ?";
        try (Connection conn = JdbcConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setLong(1, user.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Users user) {
        String query = "UPDATE users SET username = ?, profile_name = ?, picture_base64 = ? WHERE id = ?";
        try (Connection conn = JdbcConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, user.getUserName());
            stmt.setString(2, user.getProfileName());
            stmt.setString(3, user.getPictureBase64());
            stmt.setLong(4, user.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Users> findAll() {
        String query = "SELECT * FROM users";
        List<Users> usersList = new ArrayList<>();
        try (Connection conn = JdbcConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Users user = new Users(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("profile_name"),
                        rs.getString("picture_base64")
                );
                usersList.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return usersList;
    }


}
