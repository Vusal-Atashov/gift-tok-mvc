package controller;

import config.JdbcConnection;
import domain.entity.Users;
import domain.repository.impl.GiftRepositoryImpl;
import domain.repository.impl.LikeRepositoryImpl;
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
import service.impl.LikeServiceImpl;
import service.impl.UserServiceImpl;
import service.impl.WinnerSelectionServiceImpl;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

@WebServlet(urlPatterns = {"/api/v1/start-tiktok", "/api/v1/stop-tiktok", "/api/v1/winners", "/api/v1/like", "/api/v1/select-winner"})
public class TikTokControllerServlet extends HttpServlet {

    private TikTokRequest tikTokRequest;
    private WinnerSelectionService winnerSelectionService;

    @Override
    public void init() {
        tikTokRequest = new TikTokRequest();
        winnerSelectionService = new WinnerSelectionServiceImpl(
                new GiftServiceImpl(new GiftRepositoryImpl()),
                new LikeServiceImpl(new LikeRepositoryImpl()),
                new UserServiceImpl(new UserRepositoryImpl()),
                new WinningChanceRepositoryImpl());
    }
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setStatus(HttpServletResponse.SC_OK); // OPTIONS isteğine 200 OK yanıtı ver
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // CORS başlıklarını ekliyoruz
        resp.setHeader("Access-Control-Allow-Origin", "*"); // Herhangi bir origin'den gelen istekleri kabul et
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");

        String path = req.getRequestURI();
        resp.setContentType("application/json");
        PrintWriter out = resp.getWriter();

        switch (path) {
            case "/api/v1/start-tiktok" -> {
                tikTokRequest.startTracking();
                out.write("{\"message\": \"TikTok canlı takibi başladı.\"}");
            }
            case "/api/v1/stop-tiktok" -> {
                tikTokRequest.stopTracking();
                out.write("{\"message\": \"TikTok canlı takibi durduruldu.\"}");
            }
            case "/api/v1/winners" -> getWinners(out);
            case "/api/v1/like" -> getTotalLikes(resp);
            case "/api/v1/select-winner" -> selectWinner(out);
            default -> {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write("{\"error\": \"Geçersiz istek.\"}");
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

    private void getWinners(PrintWriter out) {
        JSONArray usersArray = new JSONArray();

        try (Connection conn = JdbcConnection.getConnection()) {
            String query = "SELECT u.username, u.profile_name, u.picture_base64, wc.like_count, wc.winning_chance, " +
                    "SUM(g.gift_price * g.gift_count) AS total_gift_value " +
                    "FROM users u " +
                    "JOIN winning_chances wc ON u.id = wc.user_id " +
                    "JOIN gift g ON u.id = g.user_id " +
                    "GROUP BY u.id, wc.like_count, wc.winning_chance " +
                    "ORDER BY wc.winning_chance DESC ";

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
            out.write("{\"message\": \"Kazanan bulunamadı.\"}");
        }
    }
}
