package service.impl;

import domain.entity.Gift;
import domain.entity.Users;
import domain.repository.WinningChanceRepository;
import service.GiftService;
import service.WinnerSelectionService;
import service.UserService;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class WinnerSelectionServiceImpl implements WinnerSelectionService {
    private final GiftService giftService;
    private final UserService userService;
    private final WinningChanceRepository winningChanceRepository;
    private final Random random = new Random();

    public WinnerSelectionServiceImpl(GiftService giftService, UserService userService, WinningChanceRepository winningChanceRepository) {
        this.giftService = giftService;
        this.userService = userService;
        this.winningChanceRepository = winningChanceRepository;
    }

    @Override
    public void calculateWinningChances(List<Users> users) {
        Map<Users, Integer> userPointsMap = users.stream()
                .collect(Collectors.toMap(user -> user, user -> calculateUserPoints(user.getId())));

        int totalPoints = userPointsMap.values().stream().mapToInt(Integer::intValue).sum();

        for (Users user : users) {
            int userPoints = userPointsMap.get(user);

            double percentage = (totalPoints > 0) ? (double) userPoints / totalPoints * 100 : 0;

            saveWinningChance(user, userPoints, percentage);
        }
    }
    @Override
    public Users selectWinner() {
        List<Users> users = winningChanceRepository.getUsersWithWinningChances();
        if (users.isEmpty()) {
            System.out.println("No users found with winning chances.");
            return null;
        }

        double totalWeight = users.stream()
                .mapToDouble(user -> getUserWinningChance(user.getId()))
                .sum();

        if (totalWeight == 0) {
            System.out.println("Total winning chance is zero. No winner can be selected.");
            return null;
        }

        double randomValue = random.nextDouble() * totalWeight;
        double cumulativeWeight = 0.0;

        for (Users user : users) {
            double userWinningChance = getUserWinningChance(user.getId());
            cumulativeWeight += userWinningChance;
            if (randomValue <= cumulativeWeight) {
                System.out.println("Winner is: " + user.getId());
                return user;
            }
        }

        return null;
    }

    private double getUserWinningChance(Long userId) {
        return winningChanceRepository.getWinningChanceForUser(userId);
    }

    private int calculateUserPoints(Long userId) {
        // Kullanıcıya ait tüm gift'ler toplanır
        List<Gift> userGifts = giftService.getGiftsForUser(userId);

        // Gift puanları toplanıyor
        int totalGiftPoints = userGifts.stream()
                .mapToInt(gift -> gift.getGiftPrice() * gift.getGiftCount())
                .sum();


        return totalGiftPoints ;
    }


    private void saveWinningChance(Users user, long totalGiftValue, double winningChance) {

        Users existingUser = userService.getUserById(user.getId());
        if (existingUser == null) {
            System.out.println("User " + user.getId() + " not found in users table. Attempting to save...");
            existingUser = userService.saveUser(user);
            if (existingUser == null || existingUser.getId() == null) {
                System.err.println("Failed to save user " + user.getId() + ". Skipping this user.");
                return;
            }
        }

        try {
            winningChanceRepository.saveWinningChance(
                    user.getId(),
                    totalGiftValue,
                    winningChance
            );
        } catch (Exception e) {
            System.err.println("Error saving winning chance for user ID " + user.getId() + ": " + e.getMessage());
        }
    }
}
