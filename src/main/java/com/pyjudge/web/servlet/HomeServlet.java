package com.pyjudge.web.servlet;

import com.pyjudge.model.Challenge;
import com.pyjudge.model.ContentStatus;
import com.pyjudge.model.Problem;
import com.pyjudge.model.Role;
import com.pyjudge.service.CourseOverview;
import com.pyjudge.web.BaseServlet;
import com.pyjudge.web.Ctx;
import jakarta.servlet.annotation.WebServlet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** The landing page. The empty URL pattern maps this servlet to the application root only. */
@WebServlet(name = "home", urlPatterns = "")
public class HomeServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void routes() {
        get("/", this::index);
    }

    private void index(Ctx ctx) throws Exception {
        List<Problem> approved = app.problems().listApproved();
        CourseOverview course = app.course().overview(ctx.user());
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("lessons", course.getTotal());
        stats.put("problems", app.problems().count(ContentStatus.APPROVED));
        stats.put("users", app.users().countByRole(Role.STUDENT));
        stats.put("submissions", app.submissions().countAll());

        List<Challenge> active = app.challenges().listAll(false).stream()
                .filter(c -> c.getState().equals("active")).limit(2).collect(Collectors.toList());
        ctx.set("featured", approved.subList(0, Math.min(3, approved.size())))
                .set("stats", stats)
                .set("leaders", app.settings().flag("leaderboard_enabled") ? app.stats().leaderboard(5, null, null, null) : List.of())
                .set("activeChallenges", active)
                .set("course", course)
                .set("homeTopics", course.getTopics().subList(0, Math.min(8, course.getTopics().size())));
        ctx.render("index");
    }
}
