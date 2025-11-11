package com.myanalyzer.feedbackanalyzer.user;

import com.myanalyzer.feedbackanalyzer.service.LLMService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class FeedbackController {

    @Autowired
    private FeedbackService service;

    @Autowired
    private LLMService llmService;

    @GetMapping("/AddFeedback")
    public String displayAddFeedback(Model model) {
        List<Feedback> listFeedbacks = service.listAll();
        model.addAttribute("Feedbacks", listFeedbacks);
        return "AddFeedback";
    }

    @GetMapping("/FeedbackForm")
    public String showNewForm(Model model) {
        model.addAttribute("feedback", new Feedback());
        return "FeedbackForm";
    }

    // ✅ MODIFIED: Removed AI analysis from save
    @PostMapping("/save")
    public String saveFeedback(@RequestParam("feedback") String feedbackText) {
        Feedback feedback = new Feedback();
        feedback.setFeedback(feedbackText);
        // ❌ NO AI ANALYSIS HERE
        service.save(feedback);
        return "redirect:/AddFeedback";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        Feedback feedback = service.get(id);
        if (feedback == null) {
            return "redirect:/AddFeedback";
        }
        model.addAttribute("feedback", feedback);
        return "EditFeedbackForm";
    }

    // ✅ MODIFIED: Removed AI analysis from update
    @PostMapping("/update")
    public String updateFeedback(@RequestParam("id") Integer id,
                                 @RequestParam("feedback") String feedbackText) {
        Feedback feedback = service.get(id);
        if (feedback != null) {
            feedback.setFeedback(feedbackText);
            // ❌ NO AI ANALYSIS HERE
            service.save(feedback);
        }
        return "redirect:/AddFeedback";
    }

    @GetMapping("/delete/{id}")
    public String deleteFeedback(@PathVariable("id") Integer id) {
        service.delete(id);
        return "redirect:/AddFeedback";
    }

    // ✅ NEW: Batch Analysis Endpoint
    @GetMapping("/analyzeAll")
    public String analyzeAllFeedbacks(Model model) {
        List<Feedback> allFeedbacks = service.listAll();

        if (allFeedbacks.isEmpty()) {
            model.addAttribute("error", "No feedbacks to analyze");
            return "redirect:/AddFeedback";
        }

        // Call batch analysis
        Map<String, Object> analysisResult = llmService.batchAnalyzeFeedbacks(allFeedbacks);

        // Update each feedback with sentiment
        @SuppressWarnings("unchecked")
        List<Map<String, String>> individualAnalysis =
                (List<Map<String, String>>) analysisResult.get("individualAnalysis");

        for (int i = 0; i < allFeedbacks.size() && i < individualAnalysis.size(); i++) {
            Feedback feedback = allFeedbacks.get(i);
            Map<String, String> analysis = individualAnalysis.get(i);

            feedback.setSentiment(analysis.get("sentiment"));
            feedback.setAnalysis(analysis.get("summary"));
            service.save(feedback);
        }

        // Pass data to the view
        model.addAttribute("totalFeedbacks", analysisResult.get("totalFeedbacks"));
        model.addAttribute("positiveCount", analysisResult.get("positiveCount"));
        model.addAttribute("negativeCount", analysisResult.get("negativeCount"));
        model.addAttribute("neutralCount", analysisResult.get("neutralCount"));
        model.addAttribute("positivePercentage", analysisResult.get("positivePercentage"));
        model.addAttribute("negativePercentage", analysisResult.get("negativePercentage"));
        model.addAttribute("neutralPercentage", analysisResult.get("neutralPercentage"));
        model.addAttribute("overallInsights", analysisResult.get("overallInsights"));
        model.addAttribute("feedbacks", allFeedbacks);

        return "AnalysisResults";
    }
}