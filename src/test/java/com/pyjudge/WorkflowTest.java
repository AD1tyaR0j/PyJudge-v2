package com.pyjudge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pyjudge.model.Challenge;
import com.pyjudge.model.ContentStatus;
import com.pyjudge.model.Message;
import com.pyjudge.model.Page;
import com.pyjudge.model.Problem;
import com.pyjudge.model.Role;
import com.pyjudge.model.Submission;
import com.pyjudge.model.User;
import com.pyjudge.model.Verdict;
import com.pyjudge.service.ChallengeService;
import com.pyjudge.service.Form;
import com.pyjudge.service.Outcome;
import com.pyjudge.service.StatsService.LeaderRow;
import com.pyjudge.util.PasswordHasher;
import com.pyjudge.util.Times;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The role workflows from the project brief, driven through the services against a real database. */
class WorkflowTest {
    private AppContext app;
    private User admin;
    private User teacher;
    private User student;

    @BeforeEach
    void setUp() {
        app = AppFixture.start();
        admin = AppFixture.user(app, "root", Role.ADMIN);
        teacher = AppFixture.user(app, "teacher", Role.INSTRUCTOR);
        student = AppFixture.user(app, "alice", Role.STUDENT);
    }

    @AfterEach
    void tearDown() {
        app.close();
    }

    private static Form exerciseForm(String title) {
        return Form.of("title", title, "description", "Print the answer.", "difficulty", "Medium",
                "time_limit", "3", "memory_limit", "64", "tags", "math,  loops ,", "starter_code", "");
    }

    // ------------------------------------------------------------ accounts and admin user management

    @Test
    void passwordsAreHashedAndVerified() {
        String hash = PasswordHasher.hash("s3cret-pass");
        assertFalse(hash.contains("s3cret"));
        assertTrue(PasswordHasher.verify("s3cret-pass", hash));
        assertFalse(PasswordHasher.verify("wrong", hash));
        assertFalse(PasswordHasher.verify("s3cret-pass", "not-a-hash"));
    }

    @Test
    void signUpValidationAndLogin() {
        assertFalse(app.accounts().validate("ab", "", "nope", "short", "other", null).isEmpty());
        assertEquals(List.of("That username is taken."),
                app.accounts().validate("alice", "Alice", "new@example.com", AppFixture.PASSWORD, AppFixture.PASSWORD, null));
        assertEquals(List.of("An account with that email already exists."),
                app.accounts().validate("bob", "Bob", "alice@example.com", AppFixture.PASSWORD, AppFixture.PASSWORD, null));

        assertTrue(app.accounts().authenticate("alice", AppFixture.PASSWORD).isPresent());
        assertTrue(app.accounts().authenticate("ALICE@example.com", AppFixture.PASSWORD).isPresent(), "email login ignores case");
        assertTrue(app.accounts().authenticate("alice", "wrong-password").isEmpty());
        assertTrue(app.accounts().authenticate("nobody", AppFixture.PASSWORD).isEmpty());
    }

    @Test
    void adminCreatesUpdatesAndDeletesUsers() {
        List<String> errors = app.accounts().adminCreate(Form.of("username", "newbie", "name", "New Person",
                "email", "New@Example.com", "role", "instructor", "password", "temp-pass-1"));
        assertTrue(errors.isEmpty(), errors.toString());
        User created = app.users().findByUsername("newbie").orElseThrow();
        assertEquals(Role.INSTRUCTOR, created.getRole());
        assertEquals("new@example.com", created.getEmail());

        errors = app.accounts().adminUpdate(created, admin, Form.of("name", "Renamed", "email", "renamed@example.com",
                "role", "student", "password", "", "is_blocked", "1"));
        assertTrue(errors.isEmpty(), errors.toString());
        User updated = app.users().findById(created.getId()).orElseThrow();
        assertEquals("Renamed", updated.getName());
        assertEquals(Role.STUDENT, updated.getRole());
        assertTrue(updated.isBlocked());
        assertTrue(PasswordHasher.verify("temp-pass-1", updated.getPasswordHash()), "blank password keeps the old one");

        assertEquals(1, app.users().search(Role.STUDENT, "renam").size());
        assertEquals(4, app.users().search(null, "").size());

        app.users().delete(created.getId());
        assertTrue(app.users().findById(created.getId()).isEmpty());
    }

    @Test
    void adminCannotDemoteOrBlockSelf() {
        List<String> errors = app.accounts().adminUpdate(admin, admin,
                Form.of("name", "Root", "email", "root@example.com", "role", "student"));
        assertEquals(List.of("You can't remove your own admin access or block yourself."), errors);
        assertEquals(Role.ADMIN, app.users().findById(admin.getId()).orElseThrow().getRole());
    }

    @Test
    void deletingUserRemovesTheirDataButKeepsTheirExercises() {
        Problem p = AppFixture.sumProblem(app, teacher);
        Submission sub = AppFixture.submit(app, student, p, AppFixture.SUM_SOLUTION);
        app.messaging().send(student, teacher, "Help please", sub.getId());
        app.messaging().send(teacher, student, "Sure", null);
        app.reviews().grade(sub, teacher, Form.of("grade", "90", "feedback", "Nice"));

        app.users().delete(teacher.getId());

        Problem kept = app.problems().findById(p.getId()).orElseThrow();
        assertNull(kept.getCreatedBy());
        assertNull(kept.getAuthor());
        Submission keptSub = app.submissions().findById(sub.getId()).orElseThrow();
        assertEquals(90, keptSub.getGrade());
        assertNull(keptSub.getGradedBy());
        assertTrue(app.messages().listInvolving(student.getId()).isEmpty());

        app.users().delete(student.getId());
        assertTrue(app.submissions().findById(sub.getId()).isEmpty());
    }

    @Test
    void profileAndPasswordChange() {
        assertTrue(app.accounts().updateProfile(student, "Alice A.", "alice2@example.com", "Learning Python").isEmpty());
        User saved = app.users().findById(student.getId()).orElseThrow();
        assertEquals("Alice A.", saved.getName());
        assertEquals("Learning Python", saved.getBio());
        assertFalse(app.accounts().updateProfile(student, "Alice", "teacher@example.com", "").isEmpty(), "email of someone else");

        assertEquals("Your current password is incorrect.", app.accounts().changePassword(saved, "nope", "new-pass-123", "new-pass-123"));
        assertEquals("New passwords do not match.", app.accounts().changePassword(saved, AppFixture.PASSWORD, "new-pass-123", "other"));
        assertNull(app.accounts().changePassword(saved, AppFixture.PASSWORD, "new-pass-123", "new-pass-123"));
        assertTrue(app.accounts().authenticate("alice", "new-pass-123").isPresent());
    }

    // ------------------------------------------------------------ exercise content and approval

    @Test
    void instructorExerciseNeedsApproval() {
        Problem p = app.studio().draftExercise();
        Outcome created = app.studio().createExercise(p, exerciseForm("Tricky Loops"), teacher);
        assertTrue(created.succeeded(), created.errors().toString());
        assertEquals(ContentStatus.PENDING, p.getStatus());
        assertEquals("tricky-loops", p.getSlug());
        assertEquals("math, loops", p.getTags());
        assertEquals(Problem.DEFAULT_STARTER, p.getStarterCode());
        assertTrue(app.problems().listApproved().isEmpty(), "hidden from students until approved");
        assertFalse(p.visibleTo(student));
        assertTrue(p.visibleTo(teacher));
        assertEquals(1, app.problems().listPending(5).size());

        // No test cases yet: it cannot be approved
        Outcome early = app.reviews().reviewExercise(p, admin, "approve", "");
        assertFalse(early.succeeded());

        assertTrue(app.studio().addTestCase(p, Form.of("input", "1\n", "expected_output", "1\n", "is_sample", "1")).succeeded());
        assertFalse(app.studio().addTestCase(p, Form.of("input", "1\n", "expected_output", "  ")).succeeded());
        assertEquals(1, app.testCases().countSamples(p.getId()));

        Outcome approved = app.reviews().reviewExercise(p, admin, "approve", "Looks good");
        assertTrue(approved.succeeded(), approved.errors().toString());
        Problem live = app.problems().findBySlug("tricky-loops").orElseThrow();
        assertTrue(live.isApproved());
        assertEquals(admin.getId(), live.getReviewedBy());
        assertEquals("Teacher", live.getAuthor().getDisplayName());

        List<Message> inbox = app.messages().recentReceived(teacher.getId(), 5);
        assertEquals(1, inbox.size());
        assertTrue(inbox.get(0).getBody().contains("approved and published"));
        assertTrue(inbox.get(0).getBody().contains("Looks good"));
        assertEquals(1, app.messages().countUnread(teacher.getId()));
    }

    @Test
    void rejectRequiresNoteAndEditingResubmits() {
        Problem p = app.studio().draftExercise();
        app.studio().createExercise(p, exerciseForm("Needs Work"), teacher);
        app.studio().addTestCase(p, Form.of("input", "", "expected_output", "ok"));

        assertFalse(app.reviews().reviewExercise(p, admin, "reject", "  ").succeeded());
        assertTrue(app.reviews().reviewExercise(p, admin, "reject", "Add an edge case").succeeded());
        Problem rejected = app.problems().findById(p.getId()).orElseThrow();
        assertEquals(ContentStatus.REJECTED, rejected.getStatus());
        assertEquals("Add an edge case", rejected.getReviewNote());

        Outcome saved = app.studio().updateExercise(rejected, exerciseForm("Needs Work v2"), teacher);
        assertEquals("Exercise saved and sent back to an admin for review.", saved.message());
        Problem again = app.problems().findById(p.getId()).orElseThrow();
        assertEquals(ContentStatus.PENDING, again.getStatus());
        assertNull(again.getReviewNote());
        assertEquals("needs-work-v2", again.getSlug());
    }

    @Test
    void adminExercisesPublishImmediatelyAndValidationWorks() {
        Problem p = app.studio().draftExercise();
        assertTrue(app.studio().createExercise(p, exerciseForm("Admin Made"), admin).succeeded());
        assertTrue(p.isApproved());

        Problem bad = app.studio().draftExercise();
        Outcome outcome = app.studio().createExercise(bad, Form.of("title", " ", "description", "", "difficulty", "Impossible",
                "time_limit", "99", "memory_limit", "abc"), admin);
        assertEquals(5, outcome.errors().size(), outcome.errors().toString());
        assertEquals(0, bad.getId());

        // Same title again gets a distinct slug
        Problem twin = app.studio().draftExercise();
        app.studio().createExercise(twin, exerciseForm("Admin Made"), admin);
        assertEquals("admin-made-2", twin.getSlug());
    }

    @Test
    void approvalCanBeSwitchedOffInSettings() {
        assertTrue(app.settings().update(Form.of("site_name", "CodeCamp", "announcement", "Exam on Friday",
                "registration_open", "1", "default_time_limit", "3", "default_memory_limit", "256",
                "max_pending_submissions", "5")).isEmpty());
        assertEquals("CodeCamp", app.settings().text("site_name"));
        assertFalse(app.settings().flag("require_approval"), "unticked checkbox means off");
        assertEquals(3, app.settings().number("default_time_limit"));

        Problem p = app.studio().draftExercise();
        assertEquals(256, p.getMemoryLimit());
        app.studio().createExercise(p, exerciseForm("No Review"), teacher);
        assertTrue(p.isApproved());
    }

    @Test
    void invalidSettingsAreRejected() {
        List<String> errors = app.settings().update(Form.of("site_name", "", "default_time_limit", "99",
                "default_memory_limit", "x", "max_pending_submissions", "3"));
        assertEquals(3, errors.size(), errors.toString());
        assertEquals("PyJudge", app.settings().text("site_name"), "nothing is saved when any field is invalid");
    }

    @Test
    void instructorsOnlyEditTheirOwnExercises() {
        User other = AppFixture.user(app, "other", Role.INSTRUCTOR);
        Problem p = AppFixture.sumProblem(app, teacher);
        assertTrue(p.editableBy(teacher));
        assertTrue(p.editableBy(admin));
        assertFalse(p.editableBy(other));
        assertFalse(p.editableBy(student));
        assertFalse(p.editableBy(null));
        assertEquals(1, app.problems().listByAuthor(teacher.getId(), null, "newest").size());
        assertEquals(0, app.problems().listByAuthor(other.getId(), null, "newest").size());
        assertEquals(1, app.problems().listByAuthor(null, ContentStatus.APPROVED, "title").size());
    }

    // ------------------------------------------------------------ submissions, grading, reports

    @Test
    void submissionsAreJudgedAndListed() {
        Problem p = AppFixture.sumProblem(app, teacher);
        Submission ok = AppFixture.submit(app, student, p, AppFixture.SUM_SOLUTION);
        assertEquals(Verdict.AC, ok.getVerdict());
        assertEquals(2, ok.getPassed());
        assertEquals(100, ok.getScorePercent());
        Submission wrong = AppFixture.submit(app, student, p, "print(0)\n");
        assertEquals(Verdict.WA, wrong.getVerdict());
        assertEquals("alice", wrong.getUser().getUsername());
        assertEquals(p.getTitle(), wrong.getProblem().getTitle());

        assertTrue(ok.viewableBy(student));
        assertTrue(ok.viewableBy(teacher));
        assertFalse(ok.viewableBy(AppFixture.user(app, "bob", Role.STUDENT)));

        Page<Submission> page = app.submissions().pageByUser(student.getId(), 1, 25);
        assertEquals(2, page.getTotal());
        assertEquals(wrong.getId(), page.getItems().get(0).getId(), "newest first");
        assertEquals(wrong.getId(), app.submissions().lastFor(student.getId(), p.getId()).orElseThrow().getId());
        assertEquals(java.util.Set.of(p.getId()), app.submissions().solvedProblemIds(student.getId()));
        assertEquals(50, app.submissions().acceptanceStats(null).get(p.getId()).getAcceptance());
        assertEquals(1, app.submissions().acceptanceStats(List.of(p.getId())).get(p.getId()).getSolvers());
        assertEquals(1, app.submissions().verdictCounts(student.getId()).get("AC"));

        List<LeaderRow> board = app.stats().leaderboard();
        assertEquals(1, board.size());
        assertEquals(10, board.get(0).getScore());
        assertEquals(1, app.stats().rankOf(student.getId()));
        assertNull(app.stats().rankOf(teacher.getId()));
        assertEquals(1, app.stats().streak(student.getId()));
        assertEquals(30, ((List<?>) app.stats().solveTimeline(student.getId(), 30).get("solved")).size());
        assertEquals(14, ((List<?>) app.stats().activityOverview(14).get("accepted")).size());
    }

    @Test
    void instructorGradesSubmissionAndStudentIsNotified() {
        Problem p = AppFixture.sumProblem(app, teacher);
        Submission sub = AppFixture.submit(app, student, p, AppFixture.SUM_SOLUTION);
        assertEquals(1, app.submissions().countUngradedFor(teacher.getId()));
        assertEquals(1, app.submissions().pageForAuthor(teacher.getId(), "ungraded", null, 1, 25).getTotal());

        assertFalse(app.reviews().grade(sub, teacher, Form.of("grade", "101")).succeeded());
        assertFalse(app.reviews().grade(sub, teacher, Form.of("grade", "abc")).succeeded());
        assertTrue(app.reviews().grade(sub, teacher, Form.of("grade", "85", "feedback", "Good use of map()")).succeeded());

        Submission graded = app.submissions().findById(sub.getId()).orElseThrow();
        assertEquals(85, graded.getGrade());
        assertEquals("Good use of map()", graded.getFeedback());
        assertEquals("Teacher", graded.getGrader().getDisplayName());
        assertNotNull(graded.getGradedAt());
        assertEquals(0, app.submissions().countUngradedFor(teacher.getId()));
        assertEquals(1, app.submissions().pageForAuthor(teacher.getId(), "graded", p.getId(), 1, 25).getTotal());
        assertEquals(85, app.submissions().averageGrade(student.getId()));
        assertEquals(85, app.submissions().averageGradeByProblem(List.of(p.getId())).get(p.getId()));
        assertEquals(1, app.submissions().recentFeedback(student.getId(), 5).size());
        assertEquals(1, app.submissions().reportRows(List.of(p.getId())).size());

        Message note = app.messages().recentReceived(student.getId(), 1).get(0);
        assertTrue(note.getBody().contains("was graded: 85/100"), note.getBody());
        assertEquals(sub.getId(), note.getSubmissionId());
        assertEquals(p.getTitle(), note.getSubmission().getProblem().getTitle());

        app.reviews().grade(graded, teacher, Form.of("grade", "95"));
        assertTrue(app.messages().recentReceived(student.getId(), 1).get(0).getBody().contains("regraded: 95/100"));
    }

    // ------------------------------------------------------------ messaging

    @Test
    void studentsAndInstructorsMessageEachOther() {
        assertTrue(app.messaging().send(student, teacher, "  I am stuck on loops  ", null).succeeded());
        assertTrue(app.messaging().send(teacher, student, "Try a for loop", null).succeeded());
        assertFalse(app.messaging().send(student, teacher, "   ", null).succeeded());
        assertFalse(app.messaging().send(student, null, "hello", null).succeeded());

        List<Message> thread = app.messages().thread(student.getId(), teacher.getId());
        assertEquals(2, thread.size());
        assertEquals("I am stuck on loops", thread.get(0).getBody());
        assertEquals("teacher", thread.get(0).getRecipient().getUsername());

        var inbox = app.messaging().conversations(student);
        assertEquals(1, inbox.size());
        assertEquals(1, inbox.get(0).getUnread());
        assertEquals("Try a for loop", inbox.get(0).getLast().getBody());

        app.messages().markRead(student.getId(), teacher.getId());
        assertEquals(0, app.messages().countUnread(student.getId()));
        assertEquals(1, app.messages().countUnread(teacher.getId()));
    }

    @Test
    void messagingRules() {
        User bob = AppFixture.user(app, "bob", Role.STUDENT);
        assertFalse(app.messaging().canMessage(student, bob), "students cannot message students");
        assertTrue(app.messaging().canMessage(student, admin));
        assertTrue(app.messaging().canMessage(teacher, bob));
        assertFalse(app.messaging().canMessage(student, student));
        assertEquals(2, app.messaging().contacts(student).size());
        assertEquals(3, app.messaging().contacts(teacher).size());

        app.settings().set("messaging_enabled", "0");
        assertFalse(app.messaging().canMessage(student, teacher));
        assertTrue(app.messaging().canMessage(admin, student), "admins can still reach people");
    }

    // ------------------------------------------------------------ challenges

    @Test
    void challengeProgressAndCompletion() {
        Problem first = AppFixture.sumProblem(app, teacher);
        Problem second = AppFixture.sumProblem(app, teacher);
        // Solved before the challenge starts: must not count
        Submission early = AppFixture.submit(app, student, first, AppFixture.SUM_SOLUTION);
        assertEquals(Verdict.AC, early.getVerdict());

        Challenge c = app.challengeService().draft();
        List<String> errors = app.challengeService().apply(c, Form.of("title", "Sprint", "description", "Go!",
                "start_at", Times.FORM.format(Times.now().plusMinutes(1)),
                "end_at", Times.FORM.format(Times.now().plusDays(2)),
                "problem_ids", String.valueOf(first.getId()), "problem_ids", String.valueOf(second.getId())));
        assertTrue(errors.isEmpty(), errors.toString());
        c.setCreatedBy(teacher.getId());
        app.challenges().insert(c);
        assertEquals("upcoming", c.getState());

        Challenge stored = app.challenges().findById(c.getId()).orElseThrow();
        assertEquals(2, stored.getApprovedProblems().size());
        assertEquals("Teacher", stored.getAuthor().getDisplayName());
        assertTrue(app.challengeService().progress(stored, student.getId()).isEmpty());

        // Open the window, then solve both
        stored.setStartAt(Times.now().minusHours(1));
        app.challenges().update(stored);
        AppFixture.submit(app, student, first, AppFixture.SUM_SOLUTION);
        ChallengeService.Summary half = app.challengeService().summarize(List.of(stored), student).get(0);
        assertEquals(1, half.getSolved());
        assertEquals(50, half.getPercent());
        assertFalse(half.isCompleted());
        assertFalse(app.challengeService().hasCompletedAny(student.getId()));

        AppFixture.submit(app, student, second, AppFixture.SUM_SOLUTION);
        assertTrue(app.challengeService().summarize(List.of(stored), student).get(0).isCompleted());
        assertTrue(app.challengeService().hasCompletedAny(student.getId()));
        assertEquals(20, app.stats().leaderboard(null, List.of(first.getId(), second.getId()), stored.getStartAt(),
                stored.getEndAt()).get(0).getScore());
        assertEquals(0, app.challengeService().summarize(List.of(stored), null).get(0).getSolved(), "guests");

        app.challenges().delete(stored.getId());
        assertTrue(app.challenges().findById(stored.getId()).isEmpty());
        assertTrue(app.problems().findById(first.getId()).isPresent(), "deleting a challenge keeps its exercises");
    }

    @Test
    void challengeRejectsBadInput() {
        Problem p = AppFixture.sumProblem(app, teacher);
        Challenge c = app.challengeService().draft();
        List<String> errors = app.challengeService().apply(c, Form.of("title", "", "start_at", "2030-01-02T10:00",
                "end_at", "2030-01-01T10:00"));
        assertEquals(3, errors.size(), errors.toString());
        errors = app.challengeService().apply(c, Form.of("title", "Ok", "start_at", "yesterday", "end_at", "",
                "problem_ids", String.valueOf(p.getId())));
        assertEquals(List.of("Enter valid start and end times."), errors);
    }
}
