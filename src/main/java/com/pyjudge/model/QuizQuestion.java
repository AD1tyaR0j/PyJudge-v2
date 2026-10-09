package com.pyjudge.model;

import java.util.ArrayList;
import java.util.List;

public class QuizQuestion {
    private int id;
    private int lessonId;
    private String prompt;
    private List<String> options = new ArrayList<>();
    /** Index into {@link #options} of the correct answer. */
    private int answer;
    private String explanation = "";
    private int position;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getLessonId() { return lessonId; }
    public void setLessonId(int lessonId) { this.lessonId = lessonId; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }
    public int getAnswer() { return answer; }
    public void setAnswer(int answer) { this.answer = answer; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation == null ? "" : explanation; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}
