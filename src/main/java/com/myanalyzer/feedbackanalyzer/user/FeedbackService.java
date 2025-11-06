package com.myanalyzer.feedbackanalyzer.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FeedbackService {

    @Autowired
    private FeedbackRepository repo;

    public List<Feedback> listAll() {
        return (List<Feedback>) repo.findAll();
    }

    public void save(Feedback feedback) {
        repo.save(feedback);
    }

    public Feedback get(Integer id) {
        return repo.findById(id).orElse(null);
    }


    public void delete(Integer id) {
        repo.deleteById(id);
    }
}
