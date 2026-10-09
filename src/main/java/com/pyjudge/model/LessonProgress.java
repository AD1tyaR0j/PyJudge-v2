package com.pyjudge.model;

import java.time.LocalDateTime;

public class LessonProgress {
    private int id;
    private int userId;
    private int lessonId;
    private LocalDateTime completedAt;
    /** Best quiz score so far. */
    private Integer quizScore;
    private Integer quizTotal;
    private int attempts;
    private LocalDateTime updatedAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getLessonId() { return lessonId; }
    public void setLessonId(int lessonId) { this.lessonId = lessonId; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public Integer getQuizScore() { return quizScore; }
    public void setQuizScore(Integer quizScore) { this.quizScore = quizScore; }
    public Integer getQuizTotal() { return quizTotal; }
    public void setQuizTotal(Integer quizTotal) { this.quizTotal = quizTotal; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isCompleted() {
        return completedAt != null;
    }

    public boolean isScored() {
        return quizTotal != null && quizTotal > 0;
    }

    public boolean isPerfect() {
        return isScored() && quizTotal.equals(quizScore);
    }
}
