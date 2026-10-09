package com.pyjudge.web.servlet;

import com.google.gson.JsonObject;
import com.pyjudge.config.AppConfig;
import com.pyjudge.judge.RunResult;
import com.pyjudge.judge.SandboxException;
import com.pyjudge.model.Problem;
import com.pyjudge.model.Submission;
import com.pyjudge.model.TestCase;
import com.pyjudge.model.User;
import com.pyjudge.model.Verdict;
import com.pyjudge.service.StatsService.LeaderRow;
import com.pyjudge.web.BaseServlet;
import com.pyjudge.web.Ctx;
import com.pyjudge.web.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The JSON API used by the in-browser editor: run code against custom input, submit for judging,
 * and poll a submission for its verdict. Every response, including errors, is JSON.
 */
@WebServlet(name = "api", urlPatterns = "/api/*")
public class ApiServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void routes() {
        get("/api/problems", this::listProblems);
        get("/api/problems/{slug}", this::getProblem);
        post("/api/run", this::run);
        post("/api/submit", this::submit);
        get("/api/submissions/{id}", this::getSubmission);
        get("/api/users/me/submissions", this::mySubmissions);
        get("/api/leaderboard", this::leaderboard);
    }

    private static void error(Ctx ctx, int status, String message) throws Exception {
        ctx.json(status, Map.of("error", message));
    }

    private static String validateCode(String code) {
        if (code == null || code.isBlank()) {
            return "Code cannot be empty.";
        }
        return code.length() > AppConfig.MAX_CODE_LENGTH ? "Code is too long." : null;
    }

    private static Map<String, Object> summary(Problem p) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", p.getId());
        out.put("title", p.getTitle());
        out.put("slug", p.getSlug());
        out.put("difficulty", p.getDifficulty().label());
        out.put("tags", p.getTagList());
        return out;
    }

    private void listProblems(Ctx ctx) throws Exception {
        ctx.json(HttpServletResponse.SC_OK,
                app.problems().listApproved().stream().map(ApiServlet::summary).collect(Collectors.toList()));
    }

    private void getProblem(Ctx ctx) throws Exception {
        Problem p = app.problems().findBySlug(ctx.path("slug")).filter(x -> x.visibleTo(ctx.user())).orElse(null);
        if (p == null) {
            error(ctx, HttpServletResponse.SC_NOT_FOUND, "Problem not found.");
            return;
        }
        Map<String, Object> out = summary(p);
        out.put("description", p.getDescription());
        out.put("time_limit", p.getTimeLimit());
        out.put("memory_limit", p.getMemoryLimit());
        List<Map<String, String>> samples = new ArrayList<>();
        for (TestCase tc : app.testCases().listSamples(p.getId())) { // hidden cases are never exposed
            samples.add(Map.of("input", tc.getInput(), "output", tc.getExpectedOutput()));
        }
        out.put("samples", samples);
        ctx.json(HttpServletResponse.SC_OK, out);
    }

    /** Runs code once against the given input, without judging or storing anything. */
    private void run(Ctx ctx) throws Exception {
        User user = ctx.requireUser();
        JsonObject data = Json.readObject(ctx.request());
        String code = Json.string(data, "code");
        String stdin = data.has("input") && !data.get("input").isJsonNull() ? Json.string(data, "input") : "";
        String invalid = validateCode(code);
        if (invalid != null) {
            error(ctx, HttpServletResponse.SC_BAD_REQUEST, invalid);
            return;
        }
        if (stdin == null || stdin.length() > AppConfig.MAX_INPUT_LENGTH) {
            error(ctx, HttpServletResponse.SC_BAD_REQUEST, "Input is too long.");
            return;
        }

        int timeLimit = app.settings().number("default_time_limit");
        int memoryLimit = app.settings().number("default_memory_limit");
        Integer problemId = Json.integer(data, "problem_id");
        Problem problem = problemId == null ? null
                : app.problems().findById(problemId).filter(p -> p.visibleTo(user)).orElse(null);
        if (problem != null) {
            timeLimit = problem.getTimeLimit();
            memoryLimit = problem.getMemoryLimit();
        }

        Map<String, Object> out = new LinkedHashMap<>();
        try {
            String syntaxError = app.sandbox().checkSyntax(code);
            if (syntaxError != null) {
                out.put("status", Verdict.CE.name());
                out.put("stdout", "");
                out.put("stderr", syntaxError);
                out.put("time_ms", 0);
            } else {
                RunResult result = app.sandbox().run(code, stdin, timeLimit, memoryLimit, AppConfig.MAX_OUTPUT_BYTES);
                out.put("status", result.status().name());
                out.put("stdout", result.stdout());
                out.put("stderr", result.stderr());
                out.put("time_ms", result.timeMs());
                out.put("exit_code", result.exitCode());
            }
        } catch (SandboxException e) {
            log("Could not run code", e);
            error(ctx, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        }
        ctx.json(HttpServletResponse.SC_OK, out);
    }

    /** Stores a submission and queues it for judging; the browser then polls {@code /api/submissions/{id}}. */
    private void submit(Ctx ctx) throws Exception {
        User user = ctx.requireUser();
        JsonObject data = Json.readObject(ctx.request());
        String code = Json.string(data, "code");
        String invalid = validateCode(code);
        if (invalid != null) {
            error(ctx, HttpServletResponse.SC_BAD_REQUEST, invalid);
            return;
        }
        Integer problemId = Json.integer(data, "problem_id");
        Problem problem = problemId == null ? null
                : app.problems().findById(problemId).filter(p -> p.visibleTo(user)).orElse(null);
        if (problem == null) {
            error(ctx, HttpServletResponse.SC_NOT_FOUND, "Exercise not found.");
            return;
        }
        if (app.submissions().countPending(user.getId()) >= app.settings().number("max_pending_submissions")) {
            error(ctx, 429, "You already have submissions being judged. Please wait.");
            return;
        }

        Submission sub = new Submission();
        sub.setUserId(user.getId());
        sub.setProblemId(problem.getId());
        sub.setCode(code);
        sub.setVerdict(Verdict.PENDING);
        sub.setTotal(app.testCases().count(problem.getId()));
        app.submissions().insert(sub);
        app.judge().dispatch(sub.getId());

        Submission current = app.submissions().findById(sub.getId()).orElse(sub);
        ctx.json(HttpServletResponse.SC_ACCEPTED, current.toApi());
    }

    private void getSubmission(Ctx ctx) throws Exception {
        User user = ctx.requireUser();
        Submission sub = app.submissions().findById(ctx.pathInt("id")).filter(s -> s.viewableBy(user)).orElse(null);
        if (sub == null) {
            error(ctx, HttpServletResponse.SC_NOT_FOUND, "Submission not found.");
            return;
        }
        ctx.json(HttpServletResponse.SC_OK, sub.toApi());
    }

    private void mySubmissions(Ctx ctx) throws Exception {
        User user = ctx.requireUser();
        ctx.json(HttpServletResponse.SC_OK, app.submissions().recentByUser(user.getId(), 100).stream()
                .map(Submission::toApi).collect(Collectors.toList()));
    }

    private void leaderboard(Ctx ctx) throws Exception {
        if (!app.settings().flag("leaderboard_enabled")) {
            error(ctx, HttpServletResponse.SC_NOT_FOUND, "The leaderboard is disabled.");
            return;
        }
        List<Map<String, Object>> out = new ArrayList<>();
        List<LeaderRow> rows = app.stats().leaderboard(100, null, null, null);
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rank", i + 1);
            row.put("username", rows.get(i).getUser().getUsername());
            row.put("solved", rows.get(i).getSolved());
            row.put("score", rows.get(i).getScore());
            out.add(row);
        }
        ctx.json(HttpServletResponse.SC_OK, out);
    }
}
