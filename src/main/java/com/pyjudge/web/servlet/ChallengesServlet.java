package com.pyjudge.web.servlet;

import com.pyjudge.model.Challenge;
import com.pyjudge.model.Problem;
import com.pyjudge.model.User;
import com.pyjudge.service.ChallengeService.Summary;
import com.pyjudge.web.BaseServlet;
import com.pyjudge.web.Ctx;
import com.pyjudge.web.HttpError;
import jakarta.servlet.annotation.WebServlet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Time-boxed challenges: browsing and taking part (everyone), creating and editing (instructors and admins). */
@WebServlet(name = "challenges", urlPatterns = {"/challenges", "/challenges/*"})
public class ChallengesServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void routes() {
        get("/challenges", this::index);
        get("/challenges/new", this::newForm); // registered before /challenges/{id}
        post("/challenges/new", this::create);
        get("/challenges/{id}", this::detail);
        get("/challenges/{id}/edit", this::editForm);
        post("/challenges/{id}/edit", this::update);
        post("/challenges/{id}/delete", this::delete);
    }

    private void index(Ctx ctx) throws Exception {
        List<Summary> rows = app.challengeService().summarize(app.challenges().listAll(true), ctx.user());
        Map<String, List<Summary>> groups = new LinkedHashMap<>();
        groups.put("active", new ArrayList<>());
        groups.put("upcoming", new ArrayList<>());
        groups.put("ended", new ArrayList<>());
        rows.forEach(r -> groups.get(r.getChallenge().getState()).add(r));
        ctx.set("groups", groups);
        ctx.render("challenges/index");
    }

    private Challenge find(Ctx ctx) {
        return app.challenges().findById(ctx.pathInt("id")).orElseThrow(HttpError::notFound);
    }

    private void detail(Ctx ctx) throws Exception {
        User user = ctx.user();
        Challenge c = find(ctx);
        List<Problem> problems = c.getApprovedProblems();
        Set<Integer> solved = user == null ? Set.of() : app.challengeService().progress(c, user.getId());
        List<Integer> ids = problems.stream().map(Problem::getId).collect(Collectors.toList());
        ctx.set("c", c)
                .set("problems", problems)
                .set("solved", solved)
                .set("percent", problems.isEmpty() ? 0 : Math.round(100f * solved.size() / problems.size()))
                .set("standings", app.stats().leaderboard(null, ids, c.getStartAt(), c.getEndAt()));
        ctx.render("challenges/detail");
    }

    private void showForm(Ctx ctx, Challenge c, boolean isNew) throws Exception {
        ctx.set("c", c)
                .set("isNew", isNew)
                .set("exercises", app.problems().listApproved())
                .set("selected", c.getProblems().stream().map(Problem::getId).collect(Collectors.toSet()));
        ctx.render("challenges/form");
    }

    private void newForm(Ctx ctx) throws Exception {
        ctx.requireAuthor();
        showForm(ctx, app.challengeService().draft(), true);
    }

    private void create(Ctx ctx) throws Exception {
        User user = ctx.requireAuthor();
        Challenge c = app.challengeService().draft();
        List<String> errors = app.challengeService().apply(c, ctx.form());
        if (!errors.isEmpty()) {
            ctx.flashErrors(errors);
            showForm(ctx, c, true);
            return;
        }
        c.setCreatedBy(user.getId());
        app.challenges().insert(c);
        ctx.flash("success", "Challenge created.");
        ctx.redirect("/challenges/" + c.getId());
    }

    /** The challenge, if the current user may change it: its instructor, or any admin. */
    private Challenge editable(Ctx ctx) {
        User user = ctx.requireAuthor();
        Challenge c = find(ctx);
        if (!c.editableBy(user)) {
            throw HttpError.forbidden();
        }
        return c;
    }

    private void editForm(Ctx ctx) throws Exception {
        showForm(ctx, editable(ctx), false);
    }

    private void update(Ctx ctx) throws Exception {
        Challenge c = editable(ctx);
        List<String> errors = app.challengeService().apply(c, ctx.form());
        if (!errors.isEmpty()) {
            ctx.flashErrors(errors);
            showForm(ctx, c, false);
            return;
        }
        app.challenges().update(c);
        ctx.flash("success", "Challenge saved.");
        ctx.redirect("/challenges/" + c.getId());
    }

    private void delete(Ctx ctx) throws Exception {
        Challenge c = editable(ctx);
        app.challenges().delete(c.getId());
        ctx.flash("info", "Deleted challenge “" + c.getTitle() + "”.");
        ctx.redirect("/challenges");
    }
}
