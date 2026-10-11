package com.example.interviewback.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO que contiene las estadísticas agregadas de práctica del usuario.
 */
public class InterviewStatsDto {

    private int totalSessions;
    private double averageScore;
    private int currentStreak;
    private int bestScore;
    private Map<String, Double> categoryAverages;
    private List<InterviewHistoryDto> recentSessions;

    public InterviewStatsDto() {}

    public InterviewStatsDto(int totalSessions, double averageScore, int currentStreak,
                             int bestScore, Map<String, Double> categoryAverages,
                             List<InterviewHistoryDto> recentSessions) {
        this.totalSessions = totalSessions;
        this.averageScore = averageScore;
        this.currentStreak = currentStreak;
        this.bestScore = bestScore;
        this.categoryAverages = categoryAverages;
        this.recentSessions = recentSessions;
    }

    public int getTotalSessions() { return totalSessions; }
    public void setTotalSessions(int totalSessions) { this.totalSessions = totalSessions; }

    public double getAverageScore() { return averageScore; }
    public void setAverageScore(double averageScore) { this.averageScore = averageScore; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    public int getBestScore() { return bestScore; }
    public void setBestScore(int bestScore) { this.bestScore = bestScore; }

    public Map<String, Double> getCategoryAverages() { return categoryAverages; }
    public void setCategoryAverages(Map<String, Double> categoryAverages) { this.categoryAverages = categoryAverages; }

    public List<InterviewHistoryDto> getRecentSessions() { return recentSessions; }
    public void setRecentSessions(List<InterviewHistoryDto> recentSessions) { this.recentSessions = recentSessions; }
}
