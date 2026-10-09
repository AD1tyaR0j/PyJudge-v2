package com.pyjudge.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** A coding exercise. Instructors submit them; admins approve or reject them. */
public class Problem {
    public static final String DEFAULT_STARTER = "# Read input with input() and print the answer\n";

    private int id;
    private String title = "";
    private String slug;
    private String description = "";
    private Difficulty difficulty = Difficulty.EASY;
    private int timeLimit = 2;
    private int memoryLimit = 128;
    private String tags = "";
    private String starterCode = DEFAULT_STARTER;
    private ContentStatus status = ContentStatus.PENDING;
    private String reviewNote;
    private Integer reviewedBy;
    private LocalDateTime reviewedAt;
    private Integer createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private User author;
    private User reviewer;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    public int getTimeLimit() { return timeLimit; }
    public void setTimeLimit(int timeLimit) { this.timeLimit = timeLimit; }
    public int getMemoryLimit() { return memoryLimit; }
    public void setMemoryLimit(int memoryLimit) { this.memoryLimit = memoryLimit; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags == null ? "" : tags; }
    public String getStarterCode() { return starterCode; }
    public void setStarterCode(String starterCode) { this.starterCode = starterCode; }
    public ContentStatus getStatus() { return status; }
    public void setStatus(ContentStatus status) { this.status = status; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
    public Integer getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Integer reviewedBy) { this.reviewedBy = reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
    public User getReviewer() { return reviewer; }
    public void setReviewer(User reviewer) { this.reviewer = reviewer; }

    public List<String> getTagList() {
        return splitTags(tags);
    }

    public boolean isApproved() {
        return status == ContentStatus.APPROVED;
    }

    /** Admins can edit every exercise; instructors only their own. {@code user} may be null (guest). */
    public boolean editableBy(User user) {
        if (user == null) {
            return false;
        }
        return user.isAdmin() || (user.isInstructor() && createdBy != null && createdBy == user.getId());
    }

    public boolean visibleTo(User user) {
        return isApproved() || editableBy(user);
    }

    /** "arrays,  math ," becomes ["arrays", "math"]. */
    public static List<String> splitTags(String raw) {
        List<String> out = new ArrayList<>();
        if (raw != null) {
            for (String tag : raw.split(",")) {
                if (!tag.isBlank()) {
                    out.add(tag.strip());
                }
            }
        }
        return out;
    }

    /** Normalises a comma-separated tag string for storage. */
    public static String joinTags(String raw) {
        return String.join(", ", splitTags(raw));
    }
}
