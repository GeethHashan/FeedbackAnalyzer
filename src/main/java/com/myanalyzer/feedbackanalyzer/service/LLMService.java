package com.myanalyzer.feedbackanalyzer.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class LLMService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final WebClient webClient;
    private final Gson gson = new Gson();

    public LLMService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    public String analyzeFeedback(String feedback) {
        try {
            // Create the request payload
            JsonObject requestBody = new JsonObject();

            JsonArray contents = new JsonArray();
            JsonObject content = new JsonObject();

            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text",
                    "Analyze this feedback and determine if it's POSITIVE, NEGATIVE, or NEUTRAL. " +
                            "Then provide a brief 2-3 sentence summary. Feedback: " + feedback);

            parts.add(part);
            content.add("parts", parts);
            contents.add(content);
            requestBody.add("contents", contents);

            // Make the API call
            String response = webClient.post()
                    .uri("/models/gemini-pro:generateContent?key=" + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // Parse the response
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            return jsonResponse.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Error analyzing feedback: " + e.getMessage();
        }
    }

    public String extractSentiment(String analysis) {
        String upperAnalysis = analysis.toUpperCase();
        if (upperAnalysis.contains("POSITIVE")) {
            return "POSITIVE";
        } else if (upperAnalysis.contains("NEGATIVE")) {
            return "NEGATIVE";
        } else {
            return "NEUTRAL";
        }
    }
}