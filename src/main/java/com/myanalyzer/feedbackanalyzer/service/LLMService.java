package com.myanalyzer.feedbackanalyzer.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.myanalyzer.feedbackanalyzer.user.Feedback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

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

    // ✅ NEW: Batch Analysis Method (ONE API CALL)
    public Map<String, Object> batchAnalyzeFeedbacks(List<Feedback> feedbacks) {
        try {
            // Build the prompt with all feedbacks
            StringBuilder prompt = new StringBuilder();
            prompt.append("You are a sentiment analysis API. Analyze feedbacks and return ONLY valid JSON. Do not include any markdown, explanations, or additional text.\n\n");
            prompt.append("Return EXACTLY this JSON structure:\n");
            prompt.append("{\n");
            prompt.append("  \"feedbacks\": [\n");
            prompt.append("    {\"id\": 1, \"sentiment\": \"POSITIVE\", \"summary\": \"Brief one-sentence summary\"},\n");
            prompt.append("    {\"id\": 2, \"sentiment\": \"NEGATIVE\", \"summary\": \"Brief one-sentence summary\"}\n");
            prompt.append("  ],\n");
            prompt.append("  \"overallInsights\": \"2-3 sentence overall analysis\"\n");
            prompt.append("}\n\n");
            prompt.append("Rules:\n");
            prompt.append("- Sentiment must be exactly: POSITIVE, NEGATIVE, or NEUTRAL\n");
            prompt.append("- Summary must be one sentence, no quotes inside\n");
            prompt.append("- Return ONLY the JSON, no markdown backticks or explanations\n\n");
            prompt.append("Feedbacks to analyze:\n");

            for (int i = 0; i < feedbacks.size(); i++) {
                Feedback feedback = feedbacks.get(i);
                prompt.append(String.format("%d. [ID: %d] %s\n",
                        i + 1, feedback.getId(), feedback.getFeedback()));
            }

            // Create API request
            JsonObject requestBody = new JsonObject();
            JsonArray contents = new JsonArray();
            JsonObject content = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();

            part.addProperty("text", prompt.toString());
            parts.add(part);
            content.add("parts", parts);
            contents.add(content);
            requestBody.add("contents", contents);

            // Make API call
            String response = webClient.post()
                    .uri("/models/gemini-2.5-flash:generateContent?key=" + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // Parse response
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            String aiResponse = jsonResponse.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();

            // Extract JSON from response (remove markdown code blocks and extra text)
            aiResponse = aiResponse.trim();

            // Log the raw response for debugging
            System.out.println("=== RAW AI RESPONSE ===");
            System.out.println(aiResponse);
            System.out.println("======================");

            // Remove markdown code blocks
            if (aiResponse.startsWith("```json")) {
                aiResponse = aiResponse.substring(7);
            } else if (aiResponse.startsWith("```")) {
                aiResponse = aiResponse.substring(3);
            }

            if (aiResponse.endsWith("```")) {
                aiResponse = aiResponse.substring(0, aiResponse.length() - 3);
            }

            // Find the JSON object (starts with { and ends with })
            int jsonStart = aiResponse.indexOf("{");
            int jsonEnd = aiResponse.lastIndexOf("}");

            if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                aiResponse = aiResponse.substring(jsonStart, jsonEnd + 1);
            }

            aiResponse = aiResponse.trim();

            // Parse AI response
            JsonObject analysisResult;
            try {
                analysisResult = gson.fromJson(aiResponse, JsonObject.class);
            } catch (Exception parseError) {
                System.err.println("ERROR: Failed to parse AI response as JSON");
                System.err.println("Response was: " + aiResponse);
                throw new RuntimeException("AI returned invalid JSON format. Please try again.", parseError);
            }

            JsonArray feedbacksArray = analysisResult.getAsJsonArray("feedbacks");
            String overallInsights = analysisResult.has("overallInsights")
                    ? analysisResult.get("overallInsights").getAsString()
                    : "Unable to generate insights.";

            // Build individual analysis list
            List<Map<String, String>> individualAnalysis = new ArrayList<>();
            int positiveCount = 0, negativeCount = 0, neutralCount = 0;

            for (int i = 0; i < feedbacksArray.size(); i++) {
                JsonObject fb = feedbacksArray.get(i).getAsJsonObject();
                String sentiment = fb.get("sentiment").getAsString().toUpperCase();
                String summary = fb.get("summary").getAsString();

                Map<String, String> analysis = new HashMap<>();
                analysis.put("sentiment", sentiment);
                analysis.put("summary", summary);
                individualAnalysis.add(analysis);

                // Count sentiments
                if (sentiment.equals("POSITIVE")) positiveCount++;
                else if (sentiment.equals("NEGATIVE")) negativeCount++;
                else neutralCount++;
            }

            // Calculate percentages
            int total = feedbacks.size();
            double positivePercentage = (total > 0) ? (positiveCount * 100.0 / total) : 0;
            double negativePercentage = (total > 0) ? (negativeCount * 100.0 / total) : 0;
            double neutralPercentage = (total > 0) ? (neutralCount * 100.0 / total) : 0;

            // Build result map
            Map<String, Object> result = new HashMap<>();
            result.put("individualAnalysis", individualAnalysis);
            result.put("overallInsights", overallInsights);
            result.put("totalFeedbacks", total);
            result.put("positiveCount", positiveCount);
            result.put("negativeCount", negativeCount);
            result.put("neutralCount", neutralCount);
            result.put("positivePercentage", String.format("%.1f", positivePercentage));
            result.put("negativePercentage", String.format("%.1f", negativePercentage));
            result.put("neutralPercentage", String.format("%.1f", neutralPercentage));

            return result;

        } catch (Exception e) {
            e.printStackTrace();

            // Return error result
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "Error analyzing feedbacks: " + e.getMessage());
            errorResult.put("overallInsights", "Analysis failed. Please try again.");
            errorResult.put("individualAnalysis", new ArrayList<>());
            errorResult.put("totalFeedbacks", feedbacks.size());
            errorResult.put("positiveCount", 0);
            errorResult.put("negativeCount", 0);
            errorResult.put("neutralCount", 0);
            errorResult.put("positivePercentage", "0.0");
            errorResult.put("negativePercentage", "0.0");
            errorResult.put("neutralPercentage", "0.0");

            return errorResult;
        }
    }

    // ✅ KEEP: Old single analysis method (for reference, not used anymore)
    @Deprecated
    public String analyzeFeedback(String feedback) {
        try {
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

            String response = webClient.post()
                    .uri("/models/gemini-2.5-flash:generateContent?key=" + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

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

    @Deprecated
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