package com.pyjudge.model;

public class TestCase {
    private int id;
    private int problemId;
    private String input = "";
    private String expectedOutput = "";
    private boolean sample;

    public TestCase() {
    }

    public TestCase(String input, String expectedOutput, boolean sample) {
        this.input = input;
        this.expectedOutput = expectedOutput;
        this.sample = sample;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getProblemId() { return problemId; }
    public void setProblemId(int problemId) { this.problemId = problemId; }
    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }
    public String getExpectedOutput() { return expectedOutput; }
    public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }
    public boolean isSample() { return sample; }
    public void setSample(boolean sample) { this.sample = sample; }
}
