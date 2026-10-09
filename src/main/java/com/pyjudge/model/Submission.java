package com.pyjudge.model;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class Submission {
    private int id;
    private int userId;
    private int problemId;
    private String code;
    private Verdict verdict = Verdict.PENDING;
    private int passed;
    private int total;
    private Integer runtimeMs;
    private Integer memoryKb;
    private String error;
    private LocalDateTime submittedAt;
    // Instructor review
    private Integer grade;
    private String feedback;
    private Integer gradedBy;
    private LocalDateTime gradedAt;
    private User user;
    private Problem problem;
    private User grader;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getProblemId() { return problemId; }
    public void setProblemId(int problemId) { this.problemId = problemId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Verdict getVerdict() { return verdict; }
    public void setVerdict(Verdict verdict) { this.verdict = verdict; }
    public int getPassed() { return passed; }
    public void setPassed(int passed) { this.passed = passed; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public Integer getRuntimeMs() { return runtimeMs; }
    public void setRuntimeMs(Integer runtimeMs) { this.runtimeMs = runtimeMs; }
    public Integer getMemoryKb() { return memoryKb; }
    public void setMemoryKb(Integer memoryKb) { this.memoryKb = memoryKb; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public Integer getGrade() { return grade; }
    public void setGrade(Integer grade) { this.grade = grade; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public Integer getGradedBy() { return gradedBy; }
    public void setGradedBy(Integer gradedBy) { this.gradedBy = gradedBy; }
    public LocalDateTime getGradedAt() { return gradedAt; }
    public void setGradedAt(LocalDateTime gradedAt) { this.gradedAt = gradedAt; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Problem getProblem() { return problem; }
    public void setProblem(Problem problem) { this.problem = problem; }
    public User getGrader() { return grader; }
    public void setGrader(User grader) { this.grader = grader; }

    public boolean isPending() {
        return verdict == Verdict.PENDING;
    }

    /** Share of test cases passed, 0-100. */
    public int getScorePercent() {
        return total > 0 ? Math.round(100f * passed / total) : 0;
    }

    /** The student who submitted it, and whoever may edit the exercise, can see a submission. */
    public boolean viewableBy(User viewer) {
        if (viewer == null) {
            return false;
        }
        return viewer.getId() == userId || (problem != null && problem.editableBy(viewer));
    }

    /** The JSON shape returned by the API. */
    public Map<String, Object> toApi() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("submission_id", id);
        out.put("problem_id", problemId);
        out.put("verdict", verdict.name());
        out.put("passed", passed);
        out.put("total", total);
        out.put("runtime_ms", runtimeMs);
        out.put("memory_kb", memoryKb);
        out.put("error", error);
        out.put("grade", grade);
        out.put("feedback", feedback);
        out.put("submitted_at", submittedAt == null ? null : submittedAt.toString());
        return out;
    }
}
