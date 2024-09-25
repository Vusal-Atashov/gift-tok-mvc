package domain.repository;

import domain.entity.Users;

import java.util.List;

public interface WinningChanceRepository {
    void saveWinningChance(Long userId, long totalGiftValue, double winningChance);
    List<Users> getUsersWithWinningChances();
    double getWinningChanceForUser(Long userId); // Kullanıcının kazanma şansını almak için yeni metod
}
