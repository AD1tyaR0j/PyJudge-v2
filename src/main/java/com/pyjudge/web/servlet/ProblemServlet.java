package com.pyjudge.web.servlet;

import com.pyjudge.model.Difficulty;
import com.pyjudge.model.Problem;
import com.pyjudge.model.Submission;
import com.pyjudge.model.TestCase;
import com.pyjudge.model.User;
import com.pyjudge.model.Verdict;
import com.pyjudge.web.BaseServlet;
import com.pyjudge.web.Ctx;
import com.pyjudge.web.HttpError;
import jakarta.servlet.annotation.WebServlet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** The public exercise list and the exercise page with its code editor. */
@WebServlet(name = "problems", urlPatterns = {"/problems", "/problems/*"})
public class ProblemServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void routes() {
        get("/problems", this::list);
        get("/problems/{slug}", this::detail);
    }

    private void list(Ctx ctx) throws Exception {
        Difficulty difficulty = Difficulty.fromLabel(ctx.param("difficulty"));
        String tag = ctx.param("tag");
        String q = ctx.param("q");
        User user = ctx.user();

        Set<String> allTags = new TreeSet<>();
        app.problems().listApproved().forEach(p -> allTags.addAll(p.getTagList()));

        ctx.set("problems", app.problems().searchApproved(difficulty, tag, q))
                .set("stats", app.submissions().acceptanceStats(null))
                .set("solved", user == null ? Set.of() : app.submissions().solvedProblemIds(user.getId()))
                .set("attempted", user == null ? Set.of() : app.submissions().attemptedProblemIds(user.getId()))
                .set("allTags", allTags)
                .set("selectedDifficulty", difficulty)
                .set("tag", tag)
                .set("q", q)
                .set("difficulties", Difficulty.values());
        ctx.render("problems");
    }

    private void detail(Ctx ctx) throws Exception {
        User user = ctx.user();
        // Unapproved exercises look like they do not exist, except to their author and admins
        Problem problem = app.problems().findBySlug(ctx.path("slug"))
                .filter(p -> p.visibleTo(user))
                .orElseThrow(HttpError::notFound);
        List<TestCase> samples = app.testCases().listSamples(problem.getId());
        Submission last = user == null ? null : app.submissions().lastFor(user.getId(), problem.getId()).orElse(null);

        // Data for editor.js, printed into the page as JSON
        Map<String, Object> js = new LinkedHashMap<>();
        js.put("id", problem.getId());
        js.put("slug", problem.getSlug());
        js.put("starter", problem.getStarterCode() == null ? "" : problem.getStarterCode());
        js.put("lastCode", last == null ? null : last.getCode());
        js.put("loggedIn", user != null);
        js.put("submissionUrl", ctx.request().getContextPath() + "/submissions/0");
        js.put("labels", Verdict.labels());

        ctx.set("problem", problem).set("samples", samples).set("last", last).set("problemJs", js);
        ctx.render("problem");
    }
}
