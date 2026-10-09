package com.pyjudge.model;

/** Judged submissions for one exercise, as shown in the exercise lists and reports. */
public class ExerciseStats {
    private final int submissions;
    private final int accepted;
    private final int students;
    private final int solvers;

    public ExerciseStats(int submissions, int accepted, int students, int solvers) {
        this.submissions = submissions;
        this.accepted = accepted;
        this.students = students;
        this.solvers = solvers;
    }

    public int getSubmissions() { return submissions; }
    public int getAccepted() { return accepted; }
    /** Distinct users who submitted. */
    public int getStudents() { return students; }
    /** Distinct users with an accepted submission. */
    public int getSolvers() { return solvers; }

    /** Accepted share of judged submissions (0-100), or null when nobody has submitted yet. */
    public Integer getAcceptance() {
        return submissions > 0 ? Math.round(100f * accepted / submissions) : null;
    }
}
