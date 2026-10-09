package com.pyjudge.web.servlet;

import com.pyjudge.model.Problem;
import com.pyjudge.model.Submission;
import com.pyjudge.model.User;
import com.pyjudge.web.BaseServlet;
import com.pyjudge.web.Ctx;
import com.pyjudge.web.HttpError;
import jakarta.servlet.annotation.WebServlet;

/** A student's submission history and the page for one submission (verdict, code, grade, feedback). */
@WebServlet(name = "submissions", urlPatterns = {"/submissions", "/submissions/*"})
public class SubmissionServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;
    private static final int PER_PAGE = 25;

    @Override
    protected void routes() {
        get("/submissions", this::mine);
        get("/submissions/{id}", this::detail);
    }

    private void mine(Ctx ctx) throws Exception {
        User user = ctx.requireUser();
        ctx.set("pagination", app.submissions().pageByUser(user.getId(), ctx.page(), PER_PAGE));
        ctx.render("submissions");
    }

    private void detail(Ctx ctx) throws Exception {
        User user = ctx.requireUser();
        Submission sub = app.submissions().findById(ctx.pathInt("id")).orElseThrow(HttpError::notFound);
        if (!sub.viewableBy(user)) {
            throw HttpError.forbidden();
        }
        // The full exercise, for its author (the "ask an instructor" link goes to them)
        Problem problem = app.problems().findById(sub.getProblemId()).orElseThrow(HttpError::notFound);
        sub.setProblem(problem);
        boolean owner = sub.getUserId() == user.getId();
        String askUsername = problem.getAuthor() != null && problem.getAuthor().isAuthor()
                ? problem.getAuthor().getUsername() : "";
        ctx.set("sub", sub)
                .set("isOwner", owner)
                .set("canGrade", problem.editableBy(user))
                .set("askUsername", askUsername);
        ctx.render("submission");
    }
}
