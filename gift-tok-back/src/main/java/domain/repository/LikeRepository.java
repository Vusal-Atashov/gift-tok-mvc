package domain.repository;

import domain.entity.Like;

import java.sql.Connection;
import java.sql.SQLException;

public interface LikeRepository {
    Like findByUserId(Long userId, Connection conn) throws SQLException;

    void save(Like like, Connection conn) throws SQLException;

    Like findById(long id) ;

    void update(Like like, Connection conn) throws SQLException;

    void delete(Like like, Connection conn) throws SQLException;

    Connection getConnection() throws SQLException;
}
