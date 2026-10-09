package com.pyjudge.service;

import com.pyjudge.dao.LessonDao;
import com.pyjudge.dao.MessageDao;
import com.pyjudge.dao.ProblemDao;
import com.pyjudge.dao.SubmissionDao;
import com.pyjudge.dao.TestCaseDao;
import com.pyjudge.db.Database;
import com.pyjudge.model.ContentStatus;
import com.pyjudge.model.Lesson;
import com.pyjudge.model.Problem;
import com.pyjudge.model.Submission;
import com.pyjudge.model.User;
import com.pyjudge.util.Times;

/** Decisions staff make about other people's work: approving content and grading submissions. */
public class ReviewService {
    private final Database db;
    private final ProblemDao problems;
    private final TestCaseDao testCases;
    private final LessonDao lessons;
    private final SubmissionDao submissions;
    private final MessageDao messages;

    public ReviewService(Database db, ProblemDao problems, TestCaseDao testCases, LessonDao lessons,
                         SubmissionDao submissions, MessageDao messages) {
        this.db = db;
        this.problems = problems;
        this.testCases = testCases;
        this.lessons = lessons;
        this.submissions = submissions;
        this.messages = messages;
    }

    /**
     * Approves or rejects an exercise and tells its author. The status change and the message are one
     * transaction: the author is never told about a decision that was not saved.
     *
     * @param decision {@code approve} or {@code reject}
     */
    public Outcome reviewExercise(Problem problem, User admin, String decision, String note) {
        boolean approve = "approve".equals(decision);
        String cleanNote = note == null ? "" : note.strip();
        if (!approve && cleanNote.isEmpty()) {
            return Outcome.fail("Add a note so the instructor knows what to change.");
        }
        if (approve && testCases.count(problem.getId()) == 0) {
            return Outcome.fail("This exercise has no test cases yet, so it can't be judged. Ask the instructor to add some.");
        }
        db.inTransaction(() -> {
            problem.setStatus(approve ? ContentStatus.APPROVED : ContentStatus.REJECTED);
            problem.setReviewNote(cleanNote.isEmpty() ? null : cleanNote);
            problem.setReviewedBy(admin.getId());
            problem.setReviewedAt(Times.now());
            problems.update(problem);
            notifyAuthor(problem.getCreatedBy(), admin, "exercise", problem.getTitle(), approve, cleanNote);
        });
        return Outcome.ok("“" + problem.getTitle() + "” " + (approve ? "approved" : "rejected") + ".");
    }

    public Outcome reviewLesson(Lesson lesson, User admin, String decision, String note) {
        boolean approve = "approve".equals(decision);
        String cleanNote = note == null ? "" : note.strip();
        if (!approve && cleanNote.isEmpty()) {
            return Outcome.fail("Add a note so the instructor knows what to change.");
        }
        db.inTransaction(() -> {
            lesson.setStatus(approve ? ContentStatus.APPROVED : ContentStatus.REJECTED);
            lesson.setReviewNote(cleanNote.isEmpty() ? null : cleanNote);
            lesson.setReviewedBy(admin.getId());
            lesson.setReviewedAt(Times.now());
            lessons.update(lesson);
            notifyAuthor(lesson.getCreatedBy(), admin, "lesson", lesson.getTitle(), approve, cleanNote);
        });
        return Outcome.ok("Lesson “" + lesson.getTitle() + "” " + (approve ? "approved" : "rejected") + ".");
    }

    private void notifyAuthor(Integer authorId, User admin, String kind, String title, boolean approve, String note) {
        if (authorId == null || authorId == admin.getId()) {
            return;
        }
        String body = "Your " + kind + " “" + title + "” was "
                + (approve ? "approved and published" : "returned for changes") + ".";
        if (!note.isEmpty()) {
            body += "\n\nReviewer note: " + note;
        }
        messages.insert(admin.getId(), authorId, body, null);
    }

    /** Stores a 0-100 grade with optional feedback and messages the student, as one transaction. */
    public Outcome grade(Submission sub, User grader, Form form) {
        Integer grade = form.integer("grade");
        String feedback = form.trimmed("feedback");
        if (grade == null || grade < 0 || grade > 100) {
            return Outcome.fail("Grade must be a whole number from 0 to 100.");
        }
        boolean firstTime = sub.getGrade() == null;
        db.inTransaction(() -> {
            submissions.saveGrade(sub.getId(), grade, feedback.isEmpty() ? null : feedback, grader.getId());
            String body = "Your submission #" + sub.getId() + " for “" + sub.getProblem().getTitle() + "” was "
                    + (firstTime ? "graded" : "regraded") + ": " + grade + "/100.";
            if (!feedback.isEmpty()) {
                body += "\n\n" + feedback;
            }
            messages.insert(grader.getId(), sub.getUserId(), body, sub.getId());
        });
        return Outcome.ok("Graded submission #" + sub.getId() + " and sent feedback to "
                + sub.getUser().getDisplayName() + ".");
    }
}
