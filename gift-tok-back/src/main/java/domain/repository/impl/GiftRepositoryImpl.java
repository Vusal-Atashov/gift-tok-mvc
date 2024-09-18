package domain.repository.impl;

import config.JdbcConnection;
import domain.entity.Gift;
import domain.repository.GiftRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GiftRepositoryImpl implements GiftRepository {

    @Override
    public void save(Gift gift) {
        try (Connection conn = JdbcConnection.getConnection()) {
            Optional<Gift> existingGift = findByUserIdAndName(gift.getUserId(), gift.getGiftName(), conn);

            if (existingGift.isPresent()) {
                int updatedCount = existingGift.get().getGiftCount() + gift.getGiftCount();
                updateGiftCount(gift.getUserId(), gift.getGiftName(), updatedCount, conn);
            } else {
                insertNewGift(gift, conn);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<Gift> findById(Long id) {
        String query = "SELECT * FROM gift WHERE id = ?";
        try (Connection conn = JdbcConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Gift(
                            rs.getLong("id"),
                            rs.getLong("user_id"),
                            rs.getInt("gift_price"),
                            rs.getString("gift_name"),
                            rs.getInt("gift_count")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<Gift> findAllByUserId(long userId) {
        String query = "SELECT * FROM gift WHERE user_id = ?";
        List<Gift> gifts = new ArrayList<>();
        try (Connection conn = JdbcConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    gifts.add(new Gift(
                            rs.getLong("id"),
                            rs.getLong("user_id"),
                            rs.getInt("gift_price"),
                            rs.getString("gift_name"),
                            rs.getInt("gift_count")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return gifts;
    }

    @Override
    public void deleteById(Long id) {
        String query = "DELETE FROM gift WHERE id = ?";
        try (Connection conn = JdbcConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateGiftCount(Long userId, String giftName, int giftCount, Connection conn) throws SQLException {
        String query = "UPDATE gift SET gift_count = ? WHERE user_id = ? AND gift_name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, giftCount);
            stmt.setLong(2, userId);
            stmt.setString(3, giftName);
            stmt.executeUpdate();
        }
    }

    private void insertNewGift(Gift gift, Connection conn) throws SQLException {
        String query = "INSERT INTO gift (user_id, gift_price, gift_name, gift_count) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setLong(1, gift.getUserId());
            stmt.setLong(2, gift.getGiftPrice());
            stmt.setString(3, gift.getGiftName());
            stmt.setInt(4, gift.getGiftCount());
            stmt.executeUpdate();
        }
    }

    private Optional<Gift> findByUserIdAndName(Long userId, String giftName, Connection conn) throws SQLException {
        String query = "SELECT * FROM gift WHERE user_id = ? AND gift_name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setLong(1, userId);
            stmt.setString(2, giftName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Gift(
                            rs.getLong("id"),
                            rs.getLong("user_id"),
                            rs.getInt("gift_price"),
                            rs.getString("gift_name"),
                            rs.getInt("gift_count")
                    ));
                }
            }
        }
        return Optional.empty();
    }

    public static void main(String[] args) {
        GiftRepository giftRepository = new GiftRepositoryImpl();

        System.out.println(giftRepository.findAllByUserId(16));
    }
}
