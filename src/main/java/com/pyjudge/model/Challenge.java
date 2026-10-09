package com.pyjudge.model;

import com.pyjudge.util.Times;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** A time-boxed set of exercises. Completed when a student solves them all inside the window. */
public class Challenge {
    private int id;
    private String title = "";
    private String description = "";
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer createdBy;
    private LocalDateTime createdAt;
    private User author;
    private List<Problem> problems = new ArrayList<>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description == null ? "" : description; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
    public List<Problem> getProblems() { return problems; }
    public void setProblems(List<Problem> problems) { this.problems = problems; }

    /** {@code upcoming}, {@code active} or {@code ended}. */
    public String getState() {
        LocalDateTime now = Times.now();
        if (now.isBefore(startAt)) {
            return "upcoming";
        }
        return now.isAfter(endAt) ? "ended" : "active";
    }

    public List<Problem> getApprovedProblems() {
        return problems.stream().filter(Problem::isApproved).collect(Collectors.toList());
    }

    public boolean editableBy(User user) {
        if (user == null) {
            return false;
        }
        return user.isAdmin() || (user.isInstructor() && createdBy != null && createdBy == user.getId());
    }
}
