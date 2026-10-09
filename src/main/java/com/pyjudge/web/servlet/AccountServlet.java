package com.pyjudge.web.servlet;

import com.pyjudge.model.Challenge;
import com.pyjudge.model.Problem;
import com.pyjudge.model.User;
import com.pyjudge.service.AchievementService;
import com.pyjudge.service.AchievementService.Badge;
import com.pyjudge.service.ChallengeService.Summary;
import com.pyjudge.web.BaseServlet;
import com.pyjudge.web.Ctx;
import com.pyjudge.web.Views;
import jakarta.servlet.annotation.WebServlet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Pages every logged-in user has: the dashboard router, account settings, and the student dashboard. */
@WebServlet(name = "account", urlPatterns = {"/dashboard", "/settings", "/student"})
public class AccountServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void routes() {
        get("/dashboard", this::dashboard);
        get("/settings", this::settingsForm);
        post("/settings", this::saveSettings);
        get("/student", this::studentDashboard);
    }

    /** Sends each role to its own dashboard. */
    private void dashboard(Ctx ctx) throws Exception {
        User user = ctx.requireUser();
        ctx.redirect(user.isAdmin() ? "/admin" : user.isInstructor() ? "/instructor" : "/student");
    }

    private void settingsForm(Ctx ctx) throws Exception {
        ctx.set("profile", ctx.requireUser());
        ctx.render("account/settings");
    }

    /** Profile management: name, email, bio and password. */
    private void saveSettings(Ctx ctx) throws Exception {
        User user = ctx.requireUser();
        String action = ctx.param("action");
        if (action.equals("profile")) {
            // Edit a copy so a failed validation does not change what the page header shows
            User edited = app.users().findById(user.getId()).orElseThrow();
            List<String> errors = app.accounts().updateProfile(edited, ctx.param("name"),
                    ctx.param("email").toLowerCase(Locale.ROOT), ctx.param("bio"));
            if (errors.isEmpty()) {
                ctx.flash("success", "Profile updated.");
                ctx.redirect("/settings");
                return;
            }
            ctx.flashErrors(errors);
            edited.setName(ctx.param("name"));
            edited.setEmail(ctx.param("email"));
            edited.setBio(ctx.param("bio"));
            ctx.set("profile", edited);
        } else if (action.equals("password")) {
            String error = app.accounts().changePassword(user, ctx.form().get("current_password"),
                    ctx.form().get("new_password"), ctx.form().get("confirm_password"));
            if (error == null) {
                ctx.flash("success", "Password changed.");
                ctx.redirect("/settings");
                return;
            }
            ctx.flash("error", error);
            ctx.set("profile", user);
        } else {
            ctx.set("profile", user);
        }
        ctx.render("account/settings");
    }

    private void studentDashboard(Ctx ctx) throws Exception {
        User user = ctx.requireUser();
        int uid = user.getId();
        List<Problem> approved = app.problems().listApproved();
        Set<Integer> solvedIds = app.submissions().solvedProblemIds(uid);
        Set<Integer> attemptedIds = app.submissions().attemptedProblemIds(uid);
        List<Problem> solved = approved.stream().filter(p -> solvedIds.contains(p.getId())).collect(Collectors.toList());

        // "Up next": unfinished attempts first, then exercises not tried yet, three in total
        List<Problem> inProgress = approved.stream()
                .filter(p -> attemptedIds.contains(p.getId()) && !solvedIds.contains(p.getId()))
                .collect(Collectors.toList());
        List<Problem> upNext = new ArrayList<>(inProgress.subList(0, Math.min(3, inProgress.size())));
        approved.stream().filter(p -> !attemptedIds.contains(p.getId()))
                .limit(Math.max(0, 3 - inProgress.size())).forEach(upNext::add);

        List<Challenge> started = app.challenges().listAll(false).stream()
                .filter(c -> !c.getState().equals("upcoming")).collect(Collectors.toList());
        List<Summary> challenges = app.challengeService().summarize(started, user);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("solved", solved.size());
        stats.put("total", approved.size());
        stats.put("score", Views.score(solved));
        stats.put("streak", app.stats().streak(uid));
        stats.put("rank", app.stats().rankOf(uid));
        stats.put("avgGrade", app.submissions().averageGrade(uid));
        stats.put("submissions", app.submissions().countByUser(uid));

        List<Badge> badges = app.achievements().forUser(user);
        ctx.set("stats", stats)
                .set("byDifficulty", Views.byDifficulty(approved, solvedIds))
                .set("timeline", app.stats().solveTimeline(uid, 30))
                .set("upNext", upNext)
                .set("inProgressIds", inProgress.stream().map(Problem::getId).collect(Collectors.toSet()))
                .set("active", challenges.stream()
                        .filter(s -> s.getChallenge().getState().equals("active") && !s.isCompleted()).collect(Collectors.toList()))
                .set("completed", challenges.stream().filter(Summary::isCompleted).collect(Collectors.toList()))
                .set("feedback", app.submissions().recentFeedback(uid, 5))
                .set("messages", app.messages().recentReceived(uid, 4))
                .set("course", app.course().overview(user))
                .set("badges", badges)
                .set("badgesEarned", AchievementService.earnedCount(badges));
        ctx.render("student/dashboard");
    }
}
