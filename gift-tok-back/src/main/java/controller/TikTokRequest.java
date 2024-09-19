package controller;

import config.JdbcConnection;
import domain.entity.Gift;
import domain.entity.Like;
import domain.entity.Users;
import domain.repository.impl.GiftRepositoryImpl;
import domain.repository.impl.LikeRepositoryImpl;
import domain.repository.impl.UserRepositoryImpl;
import domain.repository.impl.WinningChanceRepositoryImpl;
import io.github.jwdeveloper.tiktok.TikTokLiveClient;
import io.github.jwdeveloper.tiktok.live.LiveClient;
import service.GiftService;
import service.LikeService;
import service.UserService;
import service.impl.GiftServiceImpl;
import service.impl.LikeServiceImpl;
import service.impl.UserServiceImpl;
import service.impl.WinnerSelectionServiceImpl;
import io.github.jwdeveloper.tiktok.TikTokLive;
import io.github.jwdeveloper.tiktok.live.builder.LiveClientBuilder;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TikTokRequest {

    private final UserService userService;
    private final LikeService likeService;
    private final GiftService giftService;
    private final AtomicInteger totalLikes;
    private final WinnerSelectionServiceImpl winnerSelectionService;
    private final LiveClientBuilder client;
    private int eventCount = 0;
    private static final int BATCH_SIZE = 10;
    private final ScheduledExecutorService scheduler;
    private final BlockingQueue<Like> likeQueue = new LinkedBlockingQueue<>();
    private boolean isTracking = true;
    private boolean isConverted = true;

    public TikTokRequest() {
        this.userService = new UserServiceImpl(new UserRepositoryImpl());
        this.likeService = new LikeServiceImpl(new LikeRepositoryImpl());
        this.giftService = new GiftServiceImpl(new GiftRepositoryImpl());
        this.winnerSelectionService = new WinnerSelectionServiceImpl(giftService, likeService, userService, new WinningChanceRepositoryImpl());
        this.client = TikTokLive.newClient("ceyhun_berdeli__official");
        this.totalLikes = new AtomicInteger(0);  // totalLikes üçün düzgün təşkiledici
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::updateChances, 0, 20, TimeUnit.SECONDS);
    }

    public void connectClient() {
        int maxRetries = 5;
        int retryDelay = 5000;
        for (int i = 0; i < maxRetries; i++) {
            try {
                if (isConverted) {
                    client.buildAndConnect();
                    System.out.println("Successfully connected to TikTok Live.");
                }
                setupListeners();
                break;
            } catch (Exception e) {
                System.out.println("Failed to connect to TikTok Live: " + e.getMessage());
                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                    }
                } else {
                    System.out.println("Maximum retry attempts reached. Could not connect.");
                }
            }
        }
    }

    private void setupListeners() {
        getConnected();
        getGifts();
        getLikes();
        getError();

        client.onRoomInfo((liveClient, event) -> {
            if (!isTracking) return;
            totalLikes.set(event.getRoomInfo().getLikesCount());
        });
    }

    public void getLikes() {
        client.onLike((liveClient, event) -> {
            CompletableFuture.runAsync(() -> {
                if (!isTracking) return;

                try {
                    String name = event.getUser().getName();
                    long likes = event.getLikes();
                    System.out.println("Like added: " + likes + " by " + name);

                    Users user = userService.findUserByName(name);
                    if (user != null && user.getId() != null) {
                        Like like = new Like(user.getId(), likes);
                        likeQueue.add(like);
                        processLikeQueue();
                    } else {
                        String profileName = event.getUser().getProfileName();
                        Image picture = event.getUser().getPicture().downloadImage();
                        Users newUser = new Users(name, profileName, convertToBase64(picture));
                        Users createdUser = userService.saveUser(newUser);

                        if (createdUser != null && createdUser.getId() != null) {
                            Like like = new Like(createdUser.getId(), likes);
                            likeQueue.add(like);
                            processLikeQueue();
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Error processing likes: " + e.getMessage());
                }
            });
        });
    }

    private void processLikeQueue() {
        if (likeQueue.size() >= BATCH_SIZE) {
            List<Like> batch = new java.util.ArrayList<>();
            likeQueue.drainTo(batch, BATCH_SIZE);

            CompletableFuture.runAsync(() -> {
                for (Like like : batch) {
                    likeService.saveLike(like);
                }
                System.out.println("Processed batch of likes: " + batch.size());
            });
        }
    }

    public void getGifts() {
        client.onGift((liveClient, event) -> {
            CompletableFuture.runAsync(() -> {
                if (!isTracking) return;  // İzləmə dayandırılıbsa, hədiyyələri işləmə

                try {
                    String giftName = event.getGift().getName();
                    String name = event.getUser().getName();
                    int giftPrice = event.getGift().getDiamondCost();
                    int giftCount = event.getCombo();
                    System.out.println("Gift added");

                    Users user = userService.findUserByName(name);
                    if (user != null && user.getId() != null) {
                        Gift gift = new Gift(user.getId(), giftPrice, giftName, giftCount);
                        giftService.saveGift(gift);
                        processEvent();
                    } else {
                        String profileName = event.getUser().getProfileName();
                        Image picture = event.getUser().getPicture().downloadImage();
                        Users newUser = new Users(name, profileName, convertToBase64(picture));
                        Users createdUser = userService.saveUser(newUser);

                        if (createdUser != null && createdUser.getId() != null) {
                            Gift gift = new Gift(createdUser.getId(), giftPrice, giftName, giftCount);
                            giftService.saveGift(gift);
                            processEvent();
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Error processing gift: " + e.getMessage());
                }
            });
        });
    }

    public void processEvent() {
        eventCount++;
        if (eventCount >= BATCH_SIZE) {
            updateChances();
            eventCount = 0;
        }
    }

    public void updateChances() {
        if (!isTracking) return;  // İzləmə dayandırılıbsa, davam etmə

        List<Users> allUsers = userService.getAllUsers();
        winnerSelectionService.calculateWinningChances(allUsers);
    }



    public void getConnected() {
        client.onConnected((liveClient, event) -> {
            System.out.println("Connected to TikTok Live.");
        });
    }

    public void getDisconnected() {
        client.onDisconnected((liveClient, event) -> {
            System.out.println("Disconnected from TikTok Live.");
        });
    }

    public void getError() {
        client.onError((liveClient, event) -> {
            System.out.println("An error occurred: " + event.getException().getMessage());
        });
    }

    public String convertToBase64(Image image) {
        BufferedImage bufferedImage = toBufferedImage(image);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(bufferedImage, "png", outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            System.out.println("Error converting image to base64: " + e.getMessage());
            return null;
        }
    }

    public BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }
        BufferedImage bufferedImage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        bufferedImage.getGraphics().drawImage(img, 0, 0, null);
        return bufferedImage;
    }

    public void startTracking() {
        System.out.println("TikTok tracking started...");
        isTracking = true;
        resetDatabase();
        if (isConverted) {
            connectClient();
        }
    }

    public void stopTracking() {
        System.out.println("TikTok tracking stopped...");

        isTracking = false;
        isConverted = false;

        try {
            client.build().disconnect();
            System.out.println("Disconnected from TikTok Live.");
        } catch (Exception e) {
            System.out.println("Error while disconnecting: " + e.getMessage());
        }

        scheduler.shutdown();
    }

    public void resetDatabase() {
        String truncateWinningChances = "TRUNCATE TABLE winning_chances CASCADE";
        String truncateLikes = "TRUNCATE TABLE likes CASCADE";
        String truncateGift = "TRUNCATE TABLE gift CASCADE";
        String truncateUsers = "TRUNCATE TABLE users CASCADE";

        try (Connection connection = JdbcConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(truncateWinningChances);
            statement.execute(truncateLikes);
            statement.execute(truncateGift);
            statement.execute(truncateUsers);
            System.out.println("Database tables truncated successfully.");
        } catch (SQLException e) {
            System.out.println("Error truncating database: " + e.getMessage());
        }
    }

    public int getTotalLikes() {
        return totalLikes.get();
    }

}
