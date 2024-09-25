package controller;

import config.JdbcConnection;
import domain.entity.Gift;
import domain.entity.Users;
import domain.repository.impl.GiftRepositoryImpl;
import domain.repository.impl.UserRepositoryImpl;
import domain.repository.impl.WinningChanceRepositoryImpl;
import service.GiftService;
import service.UserService;
import service.impl.GiftServiceImpl;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TikTokRequest {

    private static final Logger logger = LoggerFactory.getLogger(TikTokRequest.class);

    private final UserService userService;
    private final GiftService giftService;
    private final AtomicInteger totalLikes = new AtomicInteger(0);
    private final WinnerSelectionServiceImpl winnerSelectionService;
    private LiveClientBuilder client;
    private int eventCount = 0;
    private static final int BATCH_SIZE = 10;
    private final ScheduledExecutorService scheduler;
    private boolean isTracking = true;
    private boolean isConnected = true;

    public TikTokRequest() {
        this.userService = new UserServiceImpl(new UserRepositoryImpl());
        this.giftService = new GiftServiceImpl(new GiftRepositoryImpl());
        this.winnerSelectionService = new WinnerSelectionServiceImpl(giftService, userService, new WinningChanceRepositoryImpl());
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::updateChances, 0, 2, TimeUnit.SECONDS);
    }

    public void connectClient() {
        if (client == null) {
            logger.error("TikTok client is not set. Please set a username first.");
            return;
        }

        int maxRetries = 5;
        int retryDelay = 5000;
        for (int i = 0; i < maxRetries; i++) {
            try {
                if (isConnected) {
                    client.buildAndConnect();
                    logger.info("Successfully connected to TikTok Live.");
                }
                setupListeners();
                break;
            } catch (Exception e) {
                logger.error("Failed to connect to TikTok Live: {}", e.getMessage());
                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException interruptedException) {
                        logger.error("Retry wait interrupted: {}", interruptedException.getMessage());
                    }
                } else {
                    logger.error("Max retries reached. Unable to connect.");
                }
            }
        }
    }

    private void setupListeners() {
        getConnected();
        getGifts();
        getError();

        client.onRoomInfo((liveClient, event) -> {
            if (!isTracking) return;
            totalLikes.set(event.getRoomInfo().getLikesCount());
        });
    }

    public void getGifts() {
        logger.info("Setting up gift listener...");
        client.onGift((liveClient, event) -> {
            CompletableFuture.runAsync(() -> {
                if (!isTracking) return;

                try {
                    String giftName = event.getGift().getName();
                    String name = event.getUser().getName();
                    int giftPrice = event.getGift().getDiamondCost();
                    int giftCount = event.getCombo();
                    logger.info("Gift added: {} worth {} diamonds by {}", giftName, giftPrice, name);

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
                    logger.error("Error processing gift: {}", e.getMessage());
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
        if (!isTracking) return;

        List<Users> allUsers = userService.getAllUsers();
        winnerSelectionService.calculateWinningChances(allUsers);
    }

    public void getConnected() {
        client.onConnected((liveClient, event) -> {
            logger.info("Connected to TikTok Live.");
        });
    }

    public void getError() {
        client.onError((liveClient, event) -> {
            logger.error("Error occurred: {}", event.getException().getMessage());
        });
    }

    public String convertToBase64(Image image) {
        BufferedImage bufferedImage = toBufferedImage(image);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(bufferedImage, "png", outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            logger.error("Error converting image to base64: {}", e.getMessage());
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

    public void addUsername(String username) {
        logger.info("Adding TikTok username: {}", username);
        this.client = TikTokLive.newClient(username);  // Username TikTok client'ı oluşturuyor
    }

    public void startTracking() {
        if (client == null) {
            logger.error("TikTok client not set. Please add a username.");
            return;
        }
        logger.info("Starting TikTok live tracking...");
        isTracking = true;
        resetDatabase();

        connectClient();  // TikTok ile bağlantı kuruluyor
    }




    public void stopTracking() {
        logger.info("TikTok tracking stopped...");

        isTracking = false;
        isConnected = false;

        try {
            client.build().disconnect();
            logger.info("Disconnected from TikTok Live.");
        } catch (Exception e) {
            logger.error("Error while disconnecting: {}", e.getMessage());
        }
        scheduler.shutdown();
    }

    public void resetDatabase() {
        String truncateWinningChances = "TRUNCATE TABLE winning_chances CASCADE";
        String truncateGift = "TRUNCATE TABLE gift CASCADE";
        String truncateUsers = "TRUNCATE TABLE users CASCADE";

        try (Connection connection = JdbcConnection.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(truncateWinningChances);
            statement.execute(truncateGift);
            statement.execute(truncateUsers);
            logger.info("Database tables truncated successfully.");
        } catch (SQLException e) {
            logger.error("Error truncating database: {}", e.getMessage());
        }
    }

    public int getTotalLikes() {
        return totalLikes.get();
    }
}
