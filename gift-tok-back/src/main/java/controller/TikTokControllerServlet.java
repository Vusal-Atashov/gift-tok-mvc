package controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import config.JdbcConnection;
import domain.entity.Users;
import domain.repository.impl.GiftRepositoryImpl;
import domain.repository.impl.UserRepositoryImpl;
import domain.repository.impl.WinningChanceRepositoryImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import service.WinnerSelectionService;
import service.impl.GiftServiceImpl;
import service.impl.UserServiceImpl;
import service.impl.WinnerSelectionServiceImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;


@WebServlet(urlPatterns = {"/api/v1/start-tiktok", "/api/v1/submit-username", "/api/v1/stop-tiktok", "/api/v1/winners", "/api/v1/like", "/api/v1/select-winner"})
public class TikTokControllerServlet extends HttpServlet {

    private TikTokRequest tikTokRequest;
    private WinnerSelectionService winnerSelectionService;
    private String currentUsername = null; // Ardışık işlem için kullanıcı adı takibi
    private static final Logger logger = LoggerFactory.getLogger(TikTokControllerServlet.class);

    @Override
    public void init() {
        tikTokRequest = new TikTokRequest();
        winnerSelectionService = new WinnerSelectionServiceImpl(
                new GiftServiceImpl(new GiftRepositoryImpl()),
                new UserServiceImpl(new UserRepositoryImpl()),
                new WinningChanceRepositoryImpl());
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");

        // İstekleri sırayla işle
        if (req.getRequestURI().endsWith("/submit-username")) {
            submitUsername(req, resp, resp.getWriter());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");

        String path = req.getRequestURI();
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();

        switch (path) {
            case "/api/v1/start-tiktok" -> {
                // TikTok sadece kullanıcı adı ayarlandığında başlatılır
                if (currentUsername != null) {
                    logger.info("Starting TikTok tracking for user: " + currentUsername);
                    tikTokRequest.startTracking();
                    out.write("{\"message\": \"TikTok live tracking started.\"}");
                } else {
                    logger.error("TikTok tracking failed: Username is not set.");
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.write("{\"error\": \"Username must be set before starting TikTok tracking.\"}");
                }

            }
            case "/api/v1/stop-tiktok" -> {
                tikTokRequest.stopTracking();
                out.write("{\"message\": \"TikTok live tracking stopped.\"}");
            }
            case "/api/v1/winners" -> getWinners(out);
            case "/api/v1/like" -> getTotalLikes(resp);
            case "/api/v1/select-winner" -> selectWinner(out);
            default -> {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("{\"error\": \"Invalid request.\"}");
            }
        }

        out.flush();
    }

    private void getTotalLikes(HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();

        CompletableFuture.runAsync(() -> {
            int likes = tikTokRequest.getTotalLikes();
            JSONObject jsonResponse = new JSONObject();
            jsonResponse.put("total_likes", likes);

            try {
                out.write(jsonResponse.toString());
                out.flush();
            } catch (Exception e) {
                System.out.println("Error sending total likes: " + e.getMessage());
            }
        });
    }

    private void submitUsername(HttpServletRequest req, HttpServletResponse resp, PrintWriter out) throws IOException {
        StringBuilder requestBody = new StringBuilder();
        String line;
        try (BufferedReader reader = req.getReader()) {
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
        }

        JSONObject jsonRequest = new JSONObject(requestBody.toString());
        String username = jsonRequest.optString("username", null);

        if (username == null || username.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write("{\"error\": \"Username is required.\"}");
            return;
        }

        tikTokRequest.addUsername(username);
        currentUsername = username; // Ardışık işlem için kullanıcı adı takip
        out.write("{\"message\": \"TikTok Live client has been set for user: " + username + "\"}");
    }

    private void getWinners(PrintWriter out) {
        JSONArray usersArray = new JSONArray();

        try (Connection conn = JdbcConnection.getConnection()) {
            String query = "SELECT u.username, u.profile_name, u.picture_base64, wc.winning_chance, " +
                    "SUM(g.gift_price * g.gift_count) AS total_gift_value " +
                    "FROM users u " +
                    "JOIN winning_chances wc ON u.id = wc.user_id " +
                    "JOIN gift g ON u.id = g.user_id " +
                    "GROUP BY u.id, u.username, u.profile_name, u.picture_base64, wc.winning_chance " +
                    "ORDER BY wc.winning_chance DESC";

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    JSONObject userObj = new JSONObject();
                    userObj.put("profile_name", rs.getString("profile_name"));
                    userObj.put("picture_base64", rs.getString("picture_base64"));
                    userObj.put("winning_chance", rs.getDouble("winning_chance"));
                    userObj.put("total_gift_value", rs.getLong("total_gift_value"));

                    usersArray.put(userObj);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        out.print(usersArray);
    }


    private void selectWinner(PrintWriter out) {
        Users winner = winnerSelectionService.selectWinner();
        if (winner != null) {
            JSONObject winnerObj = new JSONObject();
            winnerObj.put("username", winner.getUserName());
            winnerObj.put("picture_base64", winner.getPictureBase64());

            out.write(winnerObj.toString());
        } else {
            out.write("{\"message\": \"No winner found.\"}");
        }
    }
}
