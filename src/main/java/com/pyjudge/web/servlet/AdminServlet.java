package com.pyjudge.web.servlet;

import com.pyjudge.model.ContentStatus;
import com.pyjudge.model.Lesson;
import com.pyjudge.model.Problem;
import com.pyjudge.model.Role;
import com.pyjudge.model.User;
import com.pyjudge.service.Outcome;
import com.pyjudge.service.SettingsService;
import com.pyjudge.web.BaseServlet;
import com.pyjudge.web.Ctx;
import com.pyjudge.web.HttpError;
import jakarta.servlet.annotation.WebServlet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The admin area: user management, exercise content, content approval, system settings and the
 * activity overview. {@code AccessFilter} only lets admins reach these URLs.
 */
@WebServlet(name = "admin", urlPatterns = {"/admin", "/admin/*"})
public class AdminServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void routes() {
        get("/admin", this::dashboard);
        get("/admin/users", this::users);
        post("/admin/users", this::createUser);
        get("/admin/users/{id}/edit", this::editUserForm);
        post("/admin/users/{id}/edit", this::editUser);
        post("/admin/users/{id}/delete", this::deleteUser);
        get("/admin/content", this::content);
        get("/admin/approvals", this::approvals);
        post("/admin/exercises/{id}/review", this::reviewExercise);
        post("/admin/lessons/{id}/review", this::reviewLesson);
        get("/admin/settings", this::settingsForm);
        post("/admin/settings", this::saveSettings);
    }

    // ------------------------------------------------------------ dashboard

    private void dashboard(Ctx ctx) throws Exception {
        long judged = app.submissions().countJudged();
        long accepted = app.submissions().countAccepted();
        Map<String, Integer> roles = app.users().countByRole();
        long pendingExercises = app.problems().count(ContentStatus.PENDING);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("students", roles.getOrDefault(Role.STUDENT.key(), 0));
        stats.put("instructors", roles.getOrDefault(Role.INSTRUCTOR.key(), 0));
        stats.put("approved", app.problems().count(ContentStatus.APPROVED));
        stats.put("pending", pendingExercises + app.lessons().count(ContentStatus.PENDING));
        stats.put("submissions", app.submissions().countAll());
        stats.put("acceptance", judged > 0 ? Math.round(100f * accepted / judged) : 0);
        stats.put("lessons", app.lessons().count(ContentStatus.APPROVED));
        stats.put("lessonsCompleted", app.progress().countCompleted());

        List<Problem> pending = app.problems().listPending(5);
        ctx.set("stats", stats)
                .set("activity", app.stats().activityOverview(14))
                .set("pending", pending)
                .set("tests", app.testCases().countsByProblem(ids(pending)))
                .set("recent", app.submissions().recent(10));
        ctx.render("admin/dashboard");
    }

    private static List<Integer> ids(List<Problem> problems) {
        return problems.stream().map(Problem::getId).collect(Collectors.toList());
    }

    // ------------------------------------------------------------ users

    private void users(Ctx ctx) throws Exception {
        showUsers(ctx, Map.of("username", "", "name", "", "email", "", "role", Role.STUDENT.key()));
    }

    private void showUsers(Ctx ctx, Map<String, String> form) throws Exception {
        Role role = Role.fromKey(ctx.param("role"));
        String q = ctx.param("q");
        ctx.set("users", app.users().search(role, q))
                .set("counts", app.submissions().countsByUser())
                .set("form", form)
                .set("roles", Role.values())
                .set("role", role)
                .set("q", q);
        ctx.render("admin/users");
    }

    private void createUser(Ctx ctx) throws Exception {
        List<String> errors = app.accounts().adminCreate(ctx.form());
        if (errors.isEmpty()) {
            ctx.flash("success", "Created " + ctx.param("role") + " account “" + ctx.param("username") + "”.");
            ctx.redirect("/admin/users");
            return;
        }
        ctx.flashErrors(errors);
        Map<String, String> form = new LinkedHashMap<>();
        form.put("username", ctx.param("username"));
        form.put("name", ctx.param("name"));
        form.put("email", ctx.param("email").toLowerCase(Locale.ROOT));
        form.put("role", ctx.param("role"));
        showUsers(ctx, form);
    }

    private User findUser(Ctx ctx) {
        return app.users().findById(ctx.pathInt("id")).orElseThrow(HttpError::notFound);
    }

    private void editUserForm(Ctx ctx) throws Exception {
        ctx.set("target", findUser(ctx)).set("roles", Role.values());
        ctx.render("admin/user_form");
    }

    private void editUser(Ctx ctx) throws Exception {
        User target = findUser(ctx);
        List<String> errors = app.accounts().adminUpdate(target, ctx.requireUser(), ctx.form());
        if (errors.isEmpty()) {
            ctx.flash("success", "Updated “" + target.getUsername() + "”.");
            ctx.redirect("/admin/users");
            return;
        }
        ctx.flashErrors(errors);
        ctx.set("target", findUser(ctx)).set("roles", Role.values()); // reload: show the stored values again
        ctx.render("admin/user_form");
    }

    private void deleteUser(Ctx ctx) throws Exception {
        User target = findUser(ctx);
        if (target.getId() == ctx.requireUser().getId()) {
            ctx.flash("error", "You can't delete your own account.");
        } else {
            app.users().delete(target.getId());
            ctx.flash("info", "Deleted “" + target.getUsername() + "” and their submissions.");
        }
        ctx.redirect("/admin/users");
    }

    // ------------------------------------------------------------ exercise content and approvals

    private void content(Ctx ctx) throws Exception {
        ContentStatus status = ContentStatus.fromKey(ctx.param("status"));
        Map<String, Integer> counts = app.problems().countByStatus();
        ctx.set("problems", app.problems().list(status))
                .set("counts", counts)
                .set("totalCount", counts.values().stream().mapToInt(Integer::intValue).sum())
                .set("tests", app.testCases().countsByProblem(null))
                .set("status", status)
                .set("statuses", ContentStatus.values());
        ctx.render("admin/content");
    }

    private void approvals(Ctx ctx) throws Exception {
        List<Problem> pending = app.problems().listPending(Integer.MAX_VALUE);
        Map<Integer, Integer> samples = new LinkedHashMap<>();
        pending.forEach(p -> samples.put(p.getId(), app.testCases().countSamples(p.getId())));
        ctx.set("pending", pending)
                .set("tests", app.testCases().countsByProblem(ids(pending)))
                .set("samples", samples)
                .set("pendingLessons", app.lessons().listPending());
        ctx.render("admin/approvals");
    }

    private static String decision(Ctx ctx) {
        String decision = ctx.param("decision");
        if (!decision.equals("approve") && !decision.equals("reject")) {
            throw HttpError.badRequest("Choose approve or reject.");
        }
        return decision;
    }

    private void reviewExercise(Ctx ctx) throws Exception {
        Problem problem = app.problems().findById(ctx.pathInt("id")).orElseThrow(HttpError::notFound);
        Outcome outcome = app.reviews().reviewExercise(problem, ctx.requireUser(), decision(ctx), ctx.param("note"));
        ctx.flash(outcome, "success");
        ctx.redirectBack("/admin/approvals");
    }

    private void reviewLesson(Ctx ctx) throws Exception {
        Lesson lesson = app.lessons().findById(ctx.pathInt("id")).orElseThrow(HttpError::notFound);
        Outcome outcome = app.reviews().reviewLesson(lesson, ctx.requireUser(), decision(ctx), ctx.param("note"));
        ctx.flash(outcome, "success");
        ctx.redirectBack("/admin/approvals");
    }

    // ------------------------------------------------------------ system settings

    private void settingsForm(Ctx ctx) throws Exception {
        ctx.set("schema", SettingsService.SCHEMA).set("values", app.settings().all());
        ctx.render("admin/settings");
    }

    private void saveSettings(Ctx ctx) throws Exception {
        List<String> errors = app.settings().update(ctx.form());
        if (errors.isEmpty()) {
            ctx.flash("success", "Settings saved.");
            ctx.redirect("/admin/settings");
            return;
        }
        ctx.flashErrors(errors);
        settingsForm(ctx);
    }
}
