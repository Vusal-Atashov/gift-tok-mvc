package controller;

import domain.entity.Users;
import io.github.jwdeveloper.tiktok.data.settings.LiveClientSettings;
import io.github.jwdeveloper.tiktok.http.HttpClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import service.TikTokDataService;
import service.WinnerSelectionService;
import service.impl.GiftServiceImpl;
import service.impl.UserServiceImpl;
import service.impl.WinnerSelectionServiceImpl;
import domain.repository.impl.GiftRepositoryImpl;
import domain.repository.impl.UserRepositoryImpl;
import domain.repository.impl.WinningChanceRepositoryImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(urlPatterns = {"/api/v1/start-tiktok", "/api/v1/stop-tiktok", "/api/v1/winners", "/api/v1/like", "/api/v1/select-winner"})
public class TikTokControllerServlet extends HttpServlet {

    private TikTokRequest tikTokRequest;
    private TikTokDataService tikTokDataService;
    private WinnerSelectionService winnerSelectionService;
    private static final Logger logger = LoggerFactory.getLogger(TikTokControllerServlet.class);

    @Override
    public void init() {
        tikTokRequest = new TikTokRequest();
        tikTokDataService = new TikTokDataService();
        winnerSelectionService = new WinnerSelectionServiceImpl(
                new GiftServiceImpl(new GiftRepositoryImpl()),
                new UserServiceImpl(new UserRepositoryImpl()),
                new WinningChanceRepositoryImpl());
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setResponseHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setResponseHeaders(resp);
        handleRequest(req, resp, this::startTikTok);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setResponseHeaders(resp);
        handleGetRequest(req, resp);
    }

    private void handleRequest(HttpServletRequest req, HttpServletResponse resp, RequestHandler handler) throws IOException {
        try {
            handler.process(req, resp);
        } catch (Exception e) {
            logger.error("Error processing request", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("{\"error\": \"Internal server error\"}");
        }
    }

    private void handleGetRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getRequestURI();
        try (PrintWriter out = resp.getWriter()) {
            switch (path) {
                case "/api/v1/stop-tiktok" -> {
                    tikTokRequest.stopTracking();
                    writeJsonResponse(out, "{\"message\": \"TikTok live tracking stopped.\"}");
                }
                case "/api/v1/winners" -> writeJsonResponse(out, tikTokDataService.getWinners().toString());
                case "/api/v1/like" -> tikTokDataService.getTotalLikesAsync(tikTokRequest, out);
                case "/api/v1/select-winner" -> selectWinner(out);
                default -> {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    writeJsonResponse(out, "{\"error\": \"Invalid request.\"}");
                }
            }
        }
    }

    private void startTikTok(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = extractRequestBody(req).optString("username", null);

        if (username == null || username.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Username is required.\"}");
            return;
        }

        tikTokRequest.startTracking(username);

        resp.getWriter().write("{\"message\": \"TikTok live tracking started for user: " + username + "\"}");
    }

    private void selectWinner(PrintWriter out) {
        Users winner = tikTokDataService.selectWinner(winnerSelectionService);
        if (winner != null) {
            JSONObject winnerObj = new JSONObject();
            winnerObj.put("username", winner.getUserName());
            winnerObj.put("picture_base64", winner.getPictureBase64());
            writeJsonResponse(out, winnerObj.toString());
        } else {
            writeJsonResponse(out, "{\"message\": \"No winner found.\"}");
        }
    }

    private void setResponseHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void writeJsonResponse(PrintWriter out, String jsonResponse) {
        out.write(jsonResponse);
        out.flush();
    }

    private JSONObject extractRequestBody(HttpServletRequest req) throws IOException {
        StringBuilder requestBody = new StringBuilder();
        String line;
        try (BufferedReader reader = req.getReader()) {
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
        }
        return new JSONObject(requestBody.toString());
    }

    @FunctionalInterface
    private interface RequestHandler {
        void process(HttpServletRequest req, HttpServletResponse resp) throws Exception;
    }
}
