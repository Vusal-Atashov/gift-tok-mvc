package service.impl;

import domain.entity.Gift;
import domain.entity.Like;
import domain.entity.Users;
import domain.repository.WinningChanceRepository;
import service.GiftService;
import service.LikeService;
import service.WinnerSelectionService;
import service.UserService;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class WinnerSelectionServiceImpl implements WinnerSelectionService {
    private final GiftService giftService;
    private final LikeService likeService;
    private final UserService userService;
    private final WinningChanceRepository winningChanceRepository;
    private final Random random = new Random();

    public WinnerSelectionServiceImpl(GiftService giftService, LikeService likeService, UserService userService, WinningChanceRepository winningChanceRepository) {
        this.giftService = giftService;
        this.likeService = likeService;
        this.userService = userService;
        this.winningChanceRepository = winningChanceRepository;
    }

    @Override
    public void calculateWinningChances(List<Users> users) {
        // Her kullanıcının puanlarını bir kez hesaplayıp saklayan bir Map
        Map<Users, Integer> userPointsMap = users.stream()
                .collect(Collectors.toMap(user -> user, user -> calculateUserPoints(user.getId())));

        // Tüm kullanıcıların toplam puanını hesapla
        int totalPoints = userPointsMap.values().stream().mapToInt(Integer::intValue).sum();

        // Her kullanıcı için kazanma şansı hesaplanıyor
        for (Users user : users) {
            // Her kullanıcının puanı Map'ten alınır
            int userPoints = userPointsMap.get(user);

            // Kullanıcının like sayısı alınıyor
            long userLikes = getUserLikes(user);

            // Kazanma şansı yüzdesi hesaplanıyor
            double percentage = (totalPoints > 0) ? (double) userPoints / totalPoints * 100 : 0;

            // Kazanma şansı ve puan bilgisi veritabanına kaydediliyor
            saveWinningChance(user, userPoints, userLikes, percentage);
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

        double randomValue = random.nextDouble() * totalWeight;
        double cumulativeWeight = 0.0;

        for (Users user : users) {
            cumulativeWeight += getUserWinningChance(user.getId());
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
        Like userLikes = likeService.findLikeById(userId);

        // Gift puanları toplanıyor
        int totalGiftPoints = userGifts.stream()
                .mapToInt(gift -> gift.getGiftPrice() * gift.getGiftCount())
                .sum();

        // Like puanı hesaplanıyor (50 like = 1 puan)
        int totalLikePoints = (userLikes != null) ? Math.toIntExact(userLikes.getLikeCount() / 50) : 0;

        // Gift ve Like puanlarının toplamı döndürülüyor
        return totalGiftPoints + totalLikePoints;
    }

    private long getUserLikes(Users user) {
        Like userLikes = likeService.findLikeById(user.getId());
        return (userLikes != null) ? userLikes.getLikeCount() : 0;
    }

    private void saveWinningChance(Users user, long totalGiftValue, long likeCount, double winningChance) {
        // Durum 1: Eğer hediye var (gift > 0) ve like sayısı 0 ise kaydet
        if (totalGiftValue > 0 && likeCount == 0) {
            // Kaydetmeye devam et
        }
        // Durum 2: Eğer hediye var (gift > 0) ve like sayısı 1 veya daha fazla ise kaydet
        else if (totalGiftValue > 0 && likeCount >= 1) {
            // Kaydetmeye devam et
        }
        // Durum 3: Eğer hediye yok (gift = 0) ve like sayısı 200 veya daha fazlaysa kaydet
        else if (totalGiftValue == 0 && likeCount >= 200) {
            // Kaydetmeye devam et
        }
        // Diğer durumlarda kaydetme
        else {
            return;
        }

        Users existingUser = userService.getUserById(user.getId());
        if (existingUser == null) {
            System.out.println("User " + user.getId() + " not found in users table. Attempting to save...");
            existingUser = userService.saveUser(user);
            if (existingUser == null || existingUser.getId() == null) {
                System.err.println("Failed to save user " + user.getId() + ". Skipping this user.");
                return;
            }
        }

        // Kullanıcıyı veritabanına kaydetmeye çalış
        try {
            winningChanceRepository.saveWinningChance(
                    user.getId(),
                    totalGiftValue,
                    likeCount,
                    winningChance
            );
        } catch (Exception e) {
            System.err.println("Error saving winning chance for user ID " + user.getId() + ": " + e.getMessage());
        }
    }
}
