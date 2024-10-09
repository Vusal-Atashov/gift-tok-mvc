package repository;

import config.JdbcConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    public void resetDatabase() {
        String truncateWinningChances = "TRUNCATE TABLE winning_chances CASCADE";
        String truncateGift = "TRUNCATE TABLE gift CASCADE";
        String truncateUsers = "TRUNCATE TABLE users CASCADE";

        try (Connection connection = JdbcConnection.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(truncateWinningChances);
            statement.execute(truncateGift);
            statement.execute(truncateUsers);
            logger.info("Database tables truncated successfully.");
        } catch (SQLException e) {
            logger.error("Error truncating database: {}", e.getMessage());
        }
    }
}
