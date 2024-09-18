package domain.repository.impl;

import config.JdbcConnection;
import domain.entity.Like;
import domain.repository.LikeRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LikeRepositoryImpl implements LikeRepository {

    @Override
    public Like findByUserId(Long userId, Connection conn) throws SQLException {
        String query = "SELECT * FROM likes WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Like(rs.getLong("user_id"), rs.getLong("like_count"));
                }
            }
        }
        return null;
    }

    @Override
    public void save(Like like, Connection conn) throws SQLException {
        String query = "INSERT INTO likes (user_id, like_count) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setLong(1, like.getUserId());
            stmt.setLong(2, like.getLikeCount());
            stmt.executeUpdate();
        }
    }

    @Override
    public Like findById(long id) {
        try {
            String query = "SELECT * FROM likes WHERE id = ?";
            try (Connection conn = JdbcConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setLong(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return new Like(rs.getLong("user_id"), rs.getLong("like_count"));
                    }
                }
            }
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }


    @Override
    public void update(Like like, Connection conn) throws SQLException {
        String query = "UPDATE likes SET like_count = ? WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setLong(1, like.getLikeCount());
            stmt.setLong(2, like.getUserId());
            stmt.executeUpdate();
        }
    }

    @Override
    public void delete(Like like, Connection conn) throws SQLException {
        String query = "DELETE FROM likes WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setLong(1, like.getUserId());
            stmt.executeUpdate();
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return JdbcConnection.getConnection();
    }
}
