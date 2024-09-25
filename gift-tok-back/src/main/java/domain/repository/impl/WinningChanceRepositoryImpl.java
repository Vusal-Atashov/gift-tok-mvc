package domain.repository.impl;

import config.JdbcConnection;
import domain.entity.Users;
import domain.repository.WinningChanceRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WinningChanceRepositoryImpl implements WinningChanceRepository {

    private static final String UPSERT_QUERY = "INSERT INTO winning_chances (user_id, total_gift_value, winning_chance) " +
            "VALUES (?, ?, ?) " +
            "ON CONFLICT (user_id) DO UPDATE " +
            "SET total_gift_value = EXCLUDED.total_gift_value, " +
            "winning_chance = EXCLUDED.winning_chance";


    private static final String SELECT_ALL_QUERY = "SELECT u.id, u.username, u.profile_name, u.picture_base64, wc.total_gift_value, wc.winning_chance " +
            "FROM winning_chances wc " +
            "JOIN users u ON wc.user_id = u.id";

    private static final String SELECT_WINNING_CHANCE_QUERY = "SELECT winning_chance FROM winning_chances WHERE user_id = ?";

    @Override
    public void saveWinningChance(Long userId, long totalGiftValue, double winningChance) {
        try (Connection connection = JdbcConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(UPSERT_QUERY)) {

            stmt.setLong(1, userId);
            stmt.setLong(2, totalGiftValue);
            stmt.setDouble(3, winningChance);

            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error saving winning chance for user ID " + userId + ": " + e.getMessage());
        }
    }

    @Override
    public List<Users> getUsersWithWinningChances() {
        List<Users> users = new ArrayList<>();

        try (Connection connection = JdbcConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(SELECT_ALL_QUERY);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Users user = new Users();
                user.setId(rs.getLong("id"));
                user.setUserName(rs.getString("username"));
                user.setProfileName(rs.getString("profile_name"));
                user.setPictureBase64(rs.getString("picture_base64"));

                // Ek bilgi olarak total_gift_value ve winning_chance kontrolü
                double winningChance = rs.getDouble("winning_chance");
                if (winningChance > 0) {
                    users.add(user);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error fetching users with winning chances: " + e.getMessage());
        }
        return users;
    }


    @Override
    public double getWinningChanceForUser(Long userId) {
        try (Connection connection = JdbcConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(SELECT_WINNING_CHANCE_QUERY)) {

            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getDouble("winning_chance");
            }

        } catch (SQLException e) {
            System.err.println("Error fetching winning chance for user ID " + userId + ": " + e.getMessage());
        }

        return 0; // Eğer kazanma şansı yoksa 0 döner
    }
}
