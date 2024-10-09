package service;

import config.JdbcConnection;
import controller.TikTokRequest;
import domain.entity.Users;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public class TikTokDataService {

    private static final Logger logger = LoggerFactory.getLogger(TikTokDataService.class);

    public JSONArray getWinners() {
        JSONArray usersArray = new JSONArray();

        try (Connection conn = JdbcConnection.getConnection()) {
            String query = "SELECT u.username, u.profile_name, u.picture_base64, wc.winning_chance, " +
                    "SUM(g.gift_price * g.gift_count) AS total_gift_value " +
                    "FROM users u " +
                    "JOIN winning_chances wc ON u.id = wc.user_id " +
                    "JOIN gift g ON u.id = g.user_id " +
                    "GROUP BY u.id, u.username, u.profile_name, u.picture_base64, wc.winning_chance " +
                    "ORDER BY wc.winning_chance DESC";

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    JSONObject userObj = new JSONObject();
                    userObj.put("profile_name", rs.getString("profile_name"));
                    userObj.put("picture_base64", rs.getString("picture_base64"));
                    userObj.put("winning_chance", rs.getDouble("winning_chance"));
                    userObj.put("total_gift_value", rs.getLong("total_gift_value"));

                    usersArray.put(userObj);
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching winners from database", e);
        }

        return usersArray;
    }

    public Users selectWinner(WinnerSelectionService winnerSelectionService) {
        return winnerSelectionService.selectWinner();
    }

    public void getTotalLikesAsync(TikTokRequest tikTokRequest, PrintWriter out) {
        CompletableFuture.runAsync(() -> {
            int likes = tikTokRequest.getTotalLikes();
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("total_likes", likes);

            try {
                out.write(jsonResponse.toString());
                out.flush();
            } catch (Exception e) {
                logger.error("Error sending total likes", e);
            }
        });
    }
}
