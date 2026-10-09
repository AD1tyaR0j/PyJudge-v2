package com.pyjudge.model;

import java.time.LocalDateTime;
import java.util.List;

/** A tutorial page. Built-in lessons come from resources/curriculum; instructors can add more (admin-approved). */
public class Lesson {
    public static final String BUILTIN = "builtin";
    public static final String CUSTOM = "custom";

    private int id;
    private int topicId;
    private String slug;
    private String title = "";
    private String content = "";
    private int minutes = 5;
    private String practiceTags = "";
    private int position;
    private String source = CUSTOM;
    private ContentStatus status = ContentStatus.PENDING;
    private String reviewNote;
    private Integer reviewedBy;
    private LocalDateTime reviewedAt;
    private Integer createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Topic topic;
    private User author;
    private int questionCount;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getTopicId() { return topicId; }
    public void setTopicId(int topicId) { this.topicId = topicId; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getMinutes() { return minutes; }
    public void setMinutes(int minutes) { this.minutes = minutes; }
    public String getPracticeTags() { return practiceTags; }
    public void setPracticeTags(String practiceTags) { this.practiceTags = practiceTags == null ? "" : practiceTags; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
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
    public Topic getTopic() { return topic; }
    public void setTopic(Topic topic) { this.topic = topic; }
    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }

    public boolean isApproved() {
        return status == ContentStatus.APPROVED;
    }

    public boolean isBuiltin() {
        return BUILTIN.equals(source);
    }

    public List<String> getPracticeTagList() {
        return Problem.splitTags(practiceTags);
    }

    /** Admins can edit every lesson; instructors only custom lessons they wrote. */
    public boolean editableBy(User user) {
        if (user == null) {
            return false;
        }
        if (user.isAdmin()) {
            return true;
        }
        return user.isInstructor() && !isBuiltin() && createdBy != null && createdBy == user.getId();
    }

    public boolean visibleTo(User user) {
        return isApproved() || editableBy(user);
    }
}
