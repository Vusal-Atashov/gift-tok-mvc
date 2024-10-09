package controller;

import io.github.jwdeveloper.tiktok.TikTokLive;
import io.github.jwdeveloper.tiktok.live.builder.LiveClientBuilder;
import io.github.jwdeveloper.tiktok.live.LiveClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.impl.GiftServiceImpl;
import service.impl.UserServiceImpl;
import service.impl.WinnerSelectionServiceImpl;
import domain.repository.impl.GiftRepositoryImpl;
import domain.repository.impl.UserRepositoryImpl;
import domain.repository.impl.WinningChanceRepositoryImpl;
import util.ImageConverter;
import repository.DatabaseManager;

import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;
import domain.entity.Gift;
import domain.entity.Users;

public class TikTokRequest {

    private static final Logger logger = LoggerFactory.getLogger(TikTokRequest.class);

    private final GiftServiceImpl giftService;
    private final UserServiceImpl userService;
    private final WinnerSelectionServiceImpl winnerSelectionService;
    private LiveClientBuilder clientBuilder;
    private LiveClient liveClient;
    private final DatabaseManager databaseManager;
    private final ImageConverter imageConverter;
    private final AtomicInteger totalLikes = new AtomicInteger(0);
    private ScheduledExecutorService scheduler;
    private int eventCount = 0;
    private static final int BATCH_SIZE = 10;

    private String username_ = "tiktok_username";

    public TikTokRequest() {
        this.giftService = new GiftServiceImpl(new GiftRepositoryImpl());
        this.userService = new UserServiceImpl(new UserRepositoryImpl());
        this.winnerSelectionService = new WinnerSelectionServiceImpl(
                giftService,
                userService,
                new WinningChanceRepositoryImpl()
        );
        this.databaseManager = new DatabaseManager();
        this.imageConverter = new ImageConverter();
        this.scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::updateChances, 0, 2, TimeUnit.SECONDS);

        logger.info("TikTokRequest initialized with default scheduler.");
    }

    public void startTracking(String username) {
        logger.info("Starting tracking for TikTok username: {}", username);
        stopTracking(); // Yeni bir client başlatmadan önce eskiyi durdur

        // Yeni TikTok live client'i kur
        this.clientBuilder = TikTokLive.newClient(username);
        logger.info("New TikTokLive client created for username: {}", username);
        username_ = username;

        // Database veya diğer durumları sıfırla
        logger.info("Resetting database for fresh tracking session.");
        databaseManager.resetDatabase();

        // Dinleyicileri kur ve yeni client'e bağlan
        logger.info("Setting up listeners for the new TikTok live session.");
        setupListeners();
        connectClient();

        // Eğer planlayıcı durmuşsa yeniden başlat
        if (scheduler.isShutdown()) {
            logger.info("Scheduler was shut down, restarting the scheduler.");
            scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(this::updateChances, 0, 2, TimeUnit.SECONDS);
        }
    }

    public void stopTracking() {
        try {
            if (liveClient != null) {
                logger.info("Disconnecting existing live client for username: {}", username_);
                liveClient.disconnect();
                liveClient = null; // Eski client referansını temizle
            } else {
                logger.info("No active live client found to disconnect.");
            }

            if (clientBuilder != null) {
                logger.info("Clearing the client builder reference.");
                clientBuilder = null; // Client builder'ı sıfırla
            }

            logger.info("Stopped tracking for TikTok username: {}", username_);
        } catch (Exception e) {
            logger.error("Error while disconnecting live client: {}", e.getMessage());
        }

        // Eğer scheduler durmamışsa onu da kapatıyoruz
        if (!scheduler.isShutdown()) {
            logger.info("Shutting down scheduler.");
            scheduler.shutdownNow(); // Planlayıcıyı durdur
        } else {
            logger.info("Scheduler already shut down.");
        }
    }

    private void connectClient() {
        if (clientBuilder == null) {
            logger.error("Cannot connect: TikTok client builder is not set.");
            return;
        }

        int maxRetries = 5;
        int retryDelay = 5000; // milliseconds
        for (int i = 0; i < maxRetries; i++) {
            try {
                liveClient = clientBuilder.buildAndConnect();
                logger.info("Successfully connected to TikTok Live for username: {}", username_);
                break; // Exit the loop upon successful connection
            } catch (Exception e) {
                logger.error("Attempt {} to connect to TikTok Live failed: {}", (i + 1), e.getMessage());
                if (i < maxRetries - 1) {
                    logger.info("Retrying connection in {} milliseconds...", retryDelay);
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException interruptedException) {
                        logger.error("Retry wait interrupted: {}", interruptedException.getMessage());
                        Thread.currentThread().interrupt();
                    }
                } else {
                    logger.error("Max retries reached. Unable to connect to TikTok Live for username: {}", username_);
                }
            }
        }
    }

    private void setupListeners() {
        logger.info("Setting up listeners for TikTok events...");
        getConnected();
        getGifts();
        getError();

        clientBuilder.onRoomInfo((liveClient, event) -> {
            totalLikes.set(event.getRoomInfo().getLikesCount());
        });
    }

    public void getGifts() {
        logger.info("Setting up gift listener...");
        clientBuilder.onGift((liveClient, event) -> {
            try {
                String giftName = event.getGift().getName();
                String name = event.getUser().getName();
                String profileName = event.getUser().getProfileName();
                int giftPrice = event.getGift().getDiamondCost();
                int giftCount = event.getCombo();
                logger.info("Received a gift. Name: {}, Price: {}, Count: {}, User: {}", giftName, giftPrice, giftCount, profileName);

                Users user = userService.findUserByName(name);
                if (user != null && user.getId() != null) {
                    logger.info("User found: {}", name);
                    Gift gift = new Gift(user.getId(), giftPrice, giftName, giftCount);
                    giftService.saveGift(gift);
                    logger.info("Gift saved for existing user.");
                    processEvent();
                } else {
                    logger.info("User not found. Creating new user: {}", name);
                    Image picture = event.getUser().getPicture().downloadImage();
                    Users newUser = new Users(name, profileName, imageConverter.convertToBase64(picture));
                    Users createdUser = userService.saveUser(newUser);

                    if (createdUser != null && createdUser.getId() != null) {
                        logger.info("New user created: {}", name);
                        Gift gift = new Gift(createdUser.getId(), giftPrice, giftName, giftCount);
                        giftService.saveGift(gift);
                        logger.info("Gift saved for new user.");
                        processEvent();
                    }
                }
            } catch (Exception e) {
                logger.error("Error processing gift: {}", e.getMessage());
            }
        });
    }

    public void processEvent() {
        eventCount++;
        logger.info("Event processed. Current event count: {}", eventCount);
        if (eventCount >= BATCH_SIZE) {
            logger.info("Event batch size reached, updating chances...");
            updateChances();
            eventCount = 0;
        }
    }

    public void updateChances() {
        List<Users> allUsers = userService.getAllUsers();
        winnerSelectionService.calculateWinningChances(allUsers);
    }

    public void getConnected() {
        clientBuilder.onConnected((liveClient, event) -> {
            logger.info("Successfully connected to TikTok Live for username: {}", username_);
        });
    }

    public void getDisconnected() {
        clientBuilder.onDisconnected((liveClient, event) -> {
            logger.info("Disconnected from TikTok Live for username: {}", username_);
        });
    }

    public void getError() {
        clientBuilder.onError((liveClient, event) -> {
            logger.error("An error occurred: {}", event.getException().getMessage());
        });
    }

    public int getTotalLikes() {
        logger.info("Returning total likes: {}", totalLikes.get());
        return totalLikes.get();
    }
}
