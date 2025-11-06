package com.myanalyzer.feedbackanalyzer.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class FeedbackController {

    @Autowired
    private FeedbackService service;

    @GetMapping("/AddFeedback")
    public String displayAddFeedback(Model model) {
        List<Feedback> listFeedbacks = service.listAll();
        model.addAttribute("Feedbacks", listFeedbacks);
        return "AddFeedback";  // Must match AddFeedback.html
    }

    @GetMapping("/FeedbackForm")
    public String showNewForm(Model model) {
        model.addAttribute("feedback", new Feedback());
        return "FeedbackForm";
    }

    @PostMapping("/save")
    public String saveFeedback(@RequestParam("feedback") String feedbackText) {
        Feedback feedback = new Feedback();
        feedback.setFeedback(feedbackText);
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

    @PostMapping("/update")
    public String updateFeedback(@RequestParam("id") Integer id,
                                 @RequestParam("feedback") String feedbackText) {
        Feedback feedback = service.get(id);
        if (feedback != null) {
            feedback.setFeedback(feedbackText);
            service.save(feedback);
        }
        return "redirect:/AddFeedback";


    }
    @GetMapping("/delete/{id}")
    public String deleteFeedback(@PathVariable("id") Integer id) {
        service.delete(id);
        return "redirect:/AddFeedback";
    }

}
