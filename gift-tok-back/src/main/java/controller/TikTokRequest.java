package controller;

import io.github.jwdeveloper.tiktok.TikTokLive;
import io.github.jwdeveloper.tiktok.live.builder.LiveClientBuilder;
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
    private LiveClientBuilder client;
    private final DatabaseManager databaseManager;
    private final ImageConverter imageConverter;
    private final AtomicInteger totalLikes = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler;
    private int eventCount = 0;
    private static final int BATCH_SIZE = 10;

    public String username_ = "tiktok_username";

    public TikTokRequest() {
        this.giftService = new GiftServiceImpl(new GiftRepositoryImpl());
        this.userService = new UserServiceImpl(new UserRepositoryImpl());
        this.winnerSelectionService = new WinnerSelectionServiceImpl(giftService, userService, new WinningChanceRepositoryImpl());
        this.databaseManager = new DatabaseManager();
        this.imageConverter = new ImageConverter();
        this.scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::updateChances, 0, 2, TimeUnit.SECONDS);
    }

    public void startTracking(String username) {
        logger.info("Starting tracking for TikTok username: {}", username);
        this.client = TikTokLive.newClient(username);
        username_ = username;
        databaseManager.resetDatabase();
        connectClient();
    }

    public void stopTracking() {
        try {
            if (client != null) {
                client.build().disconnect();
                logger.info("Disconnected from TikTok Live.");
                client = null;
                logger.info("Is live online: {}", TikTokLive.isLiveOnline(username_));
            }
        } catch (Exception e) {
            logger.error("Error while disconnecting: {}", e.getMessage());
        }
        scheduler.shutdownNow();
    }

    private void connectClient() {
        if (client == null) {
            logger.error("TikTok client is not set.");
            return;
        }

        int maxRetries = 5;
        int retryDelay = 5000;
        for (int i = 0; i < maxRetries; i++) {
            try {
                client.buildAndConnect();
                logger.info("Successfully connected to TikTok Live.");
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
            totalLikes.set(event.getRoomInfo().getLikesCount());
        });
    }

    public void getGifts() {
        logger.info("Setting up gift listener...");
        client.onGift((liveClient, event) -> {
            try {
                String giftName = event.getGift().getName();
                String name = event.getUser().getName();
                String profileNames = event.getUser().getProfileName();
                int giftPrice = event.getGift().getDiamondCost();
                int giftCount = event.getCombo();
                logger.info("Gift added: price={}, name={}, count={}, user={}", giftPrice, giftName, giftCount, profileNames);

                Users user = userService.findUserByName(name);
                if (user != null && user.getId() != null) {
                    Gift gift = new Gift(user.getId(), giftPrice, giftName, giftCount);
                    giftService.saveGift(gift);
                    processEvent();
                } else {
                    String profileName = event.getUser().getProfileName();
                    Image picture = event.getUser().getPicture().downloadImage();
                    Users newUser = new Users(name, profileName, imageConverter.convertToBase64(picture));
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
    }

    public void processEvent() {
        eventCount++;
        if (eventCount >= BATCH_SIZE) {
            updateChances();
            eventCount = 0;
        }
    }

    public void updateChances() {
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

    public int getTotalLikes() {
        return totalLikes.get();
    }
}
