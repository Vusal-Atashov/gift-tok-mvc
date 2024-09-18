package config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class JdbcConnection {
    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5434/postgres");
        config.setUsername("postgres");
        config.setPassword("postgres");
        config.setMaximumPoolSize(30);
        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // Bağlantı qaynağını dayandırmaq üçün metod
    public static void shutdown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
