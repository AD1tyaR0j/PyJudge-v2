package com.pyjudge.web.servlet;

import com.pyjudge.dao.SubmissionDao.ReportRow;
import com.pyjudge.model.ContentStatus;
import com.pyjudge.model.ExerciseStats;
import com.pyjudge.model.Lesson;
import com.pyjudge.model.Problem;
import com.pyjudge.model.Submission;
import com.pyjudge.model.User;
import com.pyjudge.model.Verdict;
import com.pyjudge.service.Outcome;
import com.pyjudge.web.BaseServlet;
import com.pyjudge.web.Ctx;
import com.pyjudge.web.HttpError;
import jakarta.servlet.annotation.WebServlet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The instructor area: their exercises and lessons, grading student submissions, and performance
 * reports. Instructors work with their own exercises; admins see every exercise.
 */
@WebServlet(name = "instructor", urlPatterns = {"/instructor", "/instructor/*"})
public class InstructorServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;
    private static final int PER_PAGE = 25;

    @Override
    protected void routes() {
        get("/instructor", this::dashboard);
        get("/instructor/exercises", this::exercises);
        get("/instructor/lessons", this::lessons);
        get("/instructor/submissions", this::submissions);
        post("/instructor/submissions/{id}/grade", this::grade);
        get("/instructor/reports", this::reports);
    }

    /** The author whose content is shown: the instructor themselves, or null (everything) for an admin. */
    private static Integer scope(User user) {
        return user.isAdmin() ? null : user.getId();
    }

    private static List<Integer> ids(List<Problem> problems) {
        return problems.stream().map(Problem::getId).collect(Collectors.toList());
    }

    private void dashboard(Ctx ctx) throws Exception {
        User user = ctx.requireUser();
        Integer author = scope(user);
        List<Problem> problems = app.problems().listByAuthor(author, null, "oldest");
        Map<ContentStatus, Long> byStatus = problems.stream()
                .collect(Collectors.groupingBy(Problem::getStatus, Collectors.counting()));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("exercises", problems.size());
        stats.put("approved", byStatus.getOrDefault(ContentStatus.APPROVED, 0L));
        stats.put("pending", byStatus.getOrDefault(ContentStatus.PENDING, 0L));
        stats.put("rejected", byStatus.getOrDefault(ContentStatus.REJECTED, 0L));
        stats.put("submissions", app.submissions().countJudgedFor(author));
        stats.put("toGrade", app.submissions().countUngradedFor(author));
        stats.put("students", app.submissions().countStudentsFor(author));

        ctx.set("stats", stats)
                .set("toGrade", app.submissions().ungradedFor(author, 8))
                .set("inbox", app.messages().recentReceived(user.getId(), 5))
                .set("rejected", problems.stream().filter(p -> p.getStatus() == ContentStatus.REJECTED).collect(Collectors.toList()));
        ctx.render("instructor/dashboard");
    }

    private void exercises(Ctx ctx) throws Exception {
        List<Problem> problems = app.problems().listByAuthor(scope(ctx.requireUser()), null, "newest");
        List<Integer> ids = ids(problems);
        ctx.set("problems", problems)
                .set("tests", app.testCases().countsByProblem(ids))
                .set("stats", app.submissions().acceptanceStats(ids));
        ctx.render("instructor/exercises");
    }

    /** Lessons the instructor wrote (admins see the whole course), with completion statistics. */
    private void lessons(Ctx ctx) throws Exception {
        List<Lesson> rows = app.lessons().listByAuthor(scope(ctx.requireUser()));
        List<Integer> ids = rows.stream().map(Lesson::getId).collect(Collectors.toList());
        ctx.set("lessons", rows)
                .set("completions", app.progress().completionsByLesson(ids))
                .set("avgQuiz", app.progress().averageQuizByLesson(ids));
        ctx.render("instructor/lessons");
    }

    private void submissions(Ctx ctx) throws Exception {
        Integer author = scope(ctx.requireUser());
        String show = ctx.param("show").isEmpty() ? "ungraded" : ctx.param("show");
        Integer problemId = ctx.intParam("exercise");
        ctx.set("pagination", app.submissions().pageForAuthor(author, show, problemId, ctx.page(), PER_PAGE))
                .set("show", show)
                .set("problemId", problemId)
                .set("exerciseQuery", problemId == null ? "" : "&exercise=" + problemId)
                .set("exercises", app.problems().listByAuthor(author, null, "title"));
        ctx.render("instructor/submissions");
    }

    private void grade(Ctx ctx) throws Exception {
        User user = ctx.requireUser();
        Submission sub = app.submissions().findById(ctx.pathInt("id")).orElseThrow(HttpError::notFound);
        if (!sub.getProblem().editableBy(user)) {
            throw HttpError.forbidden(); // instructors grade only submissions to their own exercises
        }
        Outcome outcome = app.reviews().grade(sub, user, ctx.form());
        if (ctx.flash(outcome, "success") && !ctx.param("next").isEmpty()) {
            List<Submission> waiting = app.submissions().ungradedFor(scope(user), 1);
            if (!waiting.isEmpty()) {
                ctx.redirect("/submissions/" + waiting.get(0).getId());
                return;
            }
        }
        ctx.redirect("/submissions/" + sub.getId());
    }

    // ------------------------------------------------------------ performance reports

    /** Running totals for one student while the report is built. */
    private static final class StudentTally {
        final Set<Integer> attempted = new HashSet<>();
        final Set<Integer> solved = new HashSet<>();
        final List<Integer> grades = new ArrayList<>();
        int submissions;
        LocalDateTime last;
    }

    private void reports(Ctx ctx) throws Exception {
        User user = ctx.requireUser();
        List<Problem> problems = app.problems().listByAuthor(scope(user), ContentStatus.APPROVED, "oldest");
        List<Integer> ids = ids(problems);
        Map<Integer, ExerciseStats> stats = app.submissions().acceptanceStats(ids);
        Map<Integer, Integer> avgGrades = app.submissions().averageGradeByProblem(ids);

        List<Map<String, Object>> exerciseRows = new ArrayList<>();
        for (Problem p : problems) {
            ExerciseStats s = stats.getOrDefault(p.getId(), new ExerciseStats(0, 0, 0, 0));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("problem", p);
            row.put("submissions", s.getSubmissions());
            row.put("students", s.getStudents());
            row.put("solvers", s.getSolvers());
            row.put("acceptance", s.getAcceptance());
            row.put("avgGrade", avgGrades.get(p.getId()));
            exerciseRows.add(row);
        }

        // Per-student performance across these exercises
        Map<Integer, StudentTally> tallies = new LinkedHashMap<>();
        for (ReportRow r : app.submissions().reportRows(ids)) {
            StudentTally t = tallies.computeIfAbsent(r.userId(), id -> new StudentTally());
            t.submissions++;
            t.attempted.add(r.problemId());
            if (r.verdict() == Verdict.AC) {
                t.solved.add(r.problemId());
            }
            if (r.grade() != null) {
                t.grades.add(r.grade());
            }
            if (t.last == null || r.submittedAt().isAfter(t.last)) {
                t.last = r.submittedAt();
            }
        }
        Map<Integer, User> users = app.users().findByIds(tallies.keySet());
        List<Map<String, Object>> studentRows = new ArrayList<>();
        tallies.forEach((userId, t) -> {
            if (!users.containsKey(userId)) {
                return;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("user", users.get(userId));
            row.put("attempted", t.attempted.size());
            row.put("solved", t.solved.size());
            row.put("submissions", t.submissions);
            row.put("last", t.last);
            row.put("avgGrade", t.grades.isEmpty() ? null
                    : (int) Math.round(t.grades.stream().mapToInt(Integer::intValue).average().orElse(0)));
            row.put("progress", problems.isEmpty() ? 0 : Math.round(100f * t.solved.size() / problems.size()));
            studentRows.add(row);
        });
        studentRows.sort(Comparator.comparing((Map<String, Object> r) -> (Integer) r.get("solved")).reversed()
                .thenComparing(Comparator.comparing((Map<String, Object> r) -> (Integer) r.get("submissions")).reversed()));

        // Only attempted exercises go in the chart: "no attempts" must not read as "0% accepted"
        List<Map<String, Object>> attempted = exerciseRows.stream()
                .filter(r -> r.get("acceptance") != null).collect(Collectors.toList());
        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put("labels", attempted.stream().map(r -> ((Problem) r.get("problem")).getTitle()).collect(Collectors.toList()));
        chart.put("acceptance", attempted.stream().map(r -> r.get("acceptance")).collect(Collectors.toList()));

        ctx.set("exerciseRows", exerciseRows).set("studentRows", studentRows).set("chart", chart)
                .set("chartSize", attempted.size());
        ctx.render("instructor/reports");
    }
}
