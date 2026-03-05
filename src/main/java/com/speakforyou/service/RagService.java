package com.speakforyou.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class RagService {

    private final String baseUrl;
    private final String embeddingModel;
    private final String pgHost;
    private final int pgPort;
    private final String pgDatabase;
    private final String pgUser;
    private final String pgPassword;
    private final String pgTable;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public RagService(
            @Value("${app.dashscope.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}") String baseUrl,
            @Value("${app.embedding.model-name:text-embedding-v3}") String embeddingModel,
            @Value("${app.pgvector.host:localhost}") String pgHost,
            @Value("${app.pgvector.port:5432}") int pgPort,
            @Value("${app.pgvector.database:speak_for_you_vector}") String pgDatabase,
            @Value("${app.pgvector.user:postgres}") String pgUser,
            @Value("${app.pgvector.password:root1234}") String pgPassword,
            @Value("${app.pgvector.table:speech_embeddings}") String pgTable,
            ObjectMapper objectMapper
    ) {
        this.baseUrl = baseUrl;
        this.embeddingModel = embeddingModel;
        this.pgHost = pgHost;
        this.pgPort = pgPort;
        this.pgDatabase = pgDatabase;
        this.pgUser = pgUser;
        this.pgPassword = pgPassword;
        this.pgTable = pgTable;
        this.objectMapper = objectMapper;
    }

    public List<String> retrieveForQuickReply(String apiKey, String message, String personaName, String sceneName, int topK) {
        if (message == null || message.isBlank()) {
            return List.of();
        }
        try {
            if (apiKey == null || apiKey.isBlank()) {
                return keywordSearch(message, personaName, sceneName, topK);
            }
            List<Double> embedding = buildEmbedding(apiKey, message);
            if (embedding.isEmpty()) {
                return keywordSearch(message, personaName, sceneName, topK);
            }
            return vectorSearch(embedding, personaName, sceneName, topK);
        } catch (Exception ignored) {
            return keywordSearch(message, personaName, sceneName, topK);
        }
    }

    private List<Double> buildEmbedding(String apiKey, String text) throws Exception {
        String payload = objectMapper.writeValueAsString(
                java.util.Map.of("model", embeddingModel, "input", text)
        );
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/embeddings"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 300) {
            return List.of();
        }
        JsonNode root = objectMapper.readTree(response.body());
        JsonNode dataNode = root.path("data");
        if (!dataNode.isArray() || dataNode.isEmpty()) {
            return List.of();
        }
        JsonNode vectorNode = dataNode.get(0).path("embedding");
        if (!vectorNode.isArray() || vectorNode.isEmpty()) {
            return List.of();
        }
        List<Double> values = new ArrayList<>(vectorNode.size());
        for (JsonNode node : vectorNode) {
            values.add(node.asDouble());
        }
        return values;
    }

    private List<String> vectorSearch(List<Double> embedding, String personaName, String sceneName, int topK) {
        String vectorLiteral = toVectorLiteral(embedding);
        String sql = "SELECT text FROM " + pgTable + " " +
                "WHERE ((metadata->>'persona_type' = ?) OR (metadata->>'scene_type' = ?)) " +
                "ORDER BY embedding <=> CAST(? AS vector) ASC LIMIT ?";
        List<String> rows = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(pgUrl(), pgUser, pgPassword);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, personaName);
            ps.setString(2, sceneName);
            ps.setString(3, vectorLiteral);
            ps.setInt(4, topK);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(rs.getString("text"));
                }
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return rows;
    }

    private List<String> keywordSearch(String message, String personaName, String sceneName, int topK) {
        String[] terms = message.toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder where = new StringBuilder();
        where.append("((metadata->>'persona_type' = ?) OR (metadata->>'scene_type' = ?))");
        if (terms.length > 0) {
            where.append(" AND (");
            for (int i = 0; i < terms.length; i++) {
                if (i > 0) where.append(" OR ");
                where.append("LOWER(text) LIKE ?");
            }
            where.append(")");
        }
        String sql = "SELECT text FROM " + pgTable + " WHERE " + where + " LIMIT ?";
        List<String> rows = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(pgUrl(), pgUser, pgPassword);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, personaName);
            ps.setString(idx++, sceneName);
            for (String term : terms) {
                ps.setString(idx++, "%" + term + "%");
            }
            ps.setInt(idx, topK);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(rs.getString("text"));
                }
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return rows;
    }

    private String pgUrl() {
        return "jdbc:postgresql://" + pgHost + ":" + pgPort + "/" + pgDatabase;
    }

    private String toVectorLiteral(List<Double> embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(embedding.get(i));
        }
        sb.append(']');
        return sb.toString();
    }
}
