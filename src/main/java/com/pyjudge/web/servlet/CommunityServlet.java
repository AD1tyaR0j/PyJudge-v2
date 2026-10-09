package com.pyjudge.web.servlet;

import com.pyjudge.model.ContentStatus;
import com.pyjudge.model.Problem;
import com.pyjudge.model.User;
import com.pyjudge.service.AchievementService;
import com.pyjudge.service.AchievementService.Badge;
import com.pyjudge.web.BaseServlet;
import com.pyjudge.web.Ctx;
import com.pyjudge.web.HttpError;
import com.pyjudge.web.Views;
import jakarta.servlet.annotation.WebServlet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** The leaderboard and public profile pages. */
@WebServlet(name = "community", urlPatterns = {"/leaderboard", "/u/*"})
public class CommunityServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void routes() {
        get("/leaderboard", this::leaderboard);
        get("/u/{username}", this::profile);
    }

    private void leaderboard(Ctx ctx) throws Exception {
        if (!app.settings().flag("leaderboard_enabled")) {
            throw HttpError.notFound();
        }
        ctx.set("rows", app.stats().leaderboard());
        ctx.render("leaderboard");
    }

    private void profile(Ctx ctx) throws Exception {
        User viewer = ctx.user();
        User user = app.users().findByUsername(ctx.path("username")).orElseThrow(HttpError::notFound);
        Set<Integer> solvedIds = app.submissions().solvedProblemIds(user.getId());
        List<Problem> approved = app.problems().listApproved();
        List<Problem> solved = approved.stream().filter(p -> solvedIds.contains(p.getId())).collect(Collectors.toList());

        Map<String, Integer> verdicts = app.submissions().verdictCounts(user.getId());
        int total = verdicts.values().stream().mapToInt(Integer::intValue).sum();
        Integer acceptance = total > 0 ? Math.round(100f * verdicts.getOrDefault("AC", 0) / total) : null;

        // Recent submissions are private: the owner and staff see them, other visitors do not
        boolean own = viewer != null && (viewer.getId() == user.getId() || viewer.isAuthor());
        List<Badge> badges = user.isStudent() ? app.achievements().forUser(user) : List.of();

        ctx.set("profile", user)
                .set("solved", solved)
                .set("score", Views.score(solved))
                .set("rank", app.stats().rankOf(user.getId()))
                .set("acceptance", acceptance)
                .set("byDifficulty", Views.byDifficulty(approved, solvedIds))
                .set("verdictRows", Views.verdictRows(verdicts))
                .set("recent", own ? app.submissions().recentByUser(user.getId(), 10) : List.of())
                .set("authored", user.isAuthor()
                        ? app.problems().listByAuthor(user.getId(), ContentStatus.APPROVED, "oldest") : List.of())
                .set("course", app.course().overview(user))
                .set("badges", badges)
                .set("earnedBadges", badges.stream().filter(Badge::isEarned).collect(Collectors.toList()))
                .set("badgesEarned", AchievementService.earnedCount(badges));
        ctx.render("profile");
    }
}
