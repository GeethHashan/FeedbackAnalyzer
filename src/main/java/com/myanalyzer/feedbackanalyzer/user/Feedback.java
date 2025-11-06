package com.myanalyzer.feedbackanalyzer.user;

import jakarta.persistence.*;

@Entity
@Table(name = "feedbacks")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 1000)  // 👈 allows longer text
    private String feedback;

    private Integer version;

    // ✅ ADD THIS FIELD
    @Column(length = 2000)
    private String analysis;

    // ✅ ADD THIS FIELD
    private String sentiment;  // POSITIVE, NEGATIVE, NEUTRAL



    // ✅ Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public String getAnalysis() { return analysis; }
    public void setAnalysis(String analysis) { this.analysis = analysis; }

    public String getSentiment() { return sentiment; }
    public void setSentiment(String sentiment) { this.sentiment = sentiment; }


}
