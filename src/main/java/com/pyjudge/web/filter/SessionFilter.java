package com.pyjudge.web.filter;

import com.pyjudge.AppContext;
import com.pyjudge.model.User;
import com.pyjudge.model.Verdict;
import com.pyjudge.util.Times;
import com.pyjudge.web.BaseServlet;
import com.pyjudge.web.Ctx;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * Prepares every page request: looks up the logged-in user from the session, and exposes the
 * things the shared layout needs (site settings, unread message count, CSRF token, nav section).
 */
public class SessionFilter extends HttpFilter {
    private static final long serialVersionUID = 1L;
    public static final String CSRF_TOKEN = "csrfToken";
    private static final int LAST_SEEN_MINUTES = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        AppContext app = (AppContext) getServletContext().getAttribute(AppContext.ATTRIBUTE);
        String path = BaseServlet.appPath(req);
        if (app == null || path.startsWith("/static/")) {
            chain.doFilter(req, resp); // static files need no session; a failed start is reported by the servlets
            return;
        }

        HttpSession session = req.getSession();
        User user = null;
        if (session.getAttribute(Ctx.SESSION_USER_ID) instanceof Integer userId) {
            // Loaded on every request so a blocked or deleted account loses access immediately
            user = app.users().findById(userId).filter(u -> !u.isBlocked()).orElse(null);
            if (user == null) {
                session.removeAttribute(Ctx.SESSION_USER_ID);
            }
        }
        if (session.getAttribute(CSRF_TOKEN) == null) {
            byte[] token = new byte[16];
            RANDOM.nextBytes(token);
            session.setAttribute(CSRF_TOKEN, HexFormat.of().formatHex(token));
        }

        req.setAttribute("ctx", req.getContextPath());
        req.setAttribute("path", path);
        req.setAttribute("section", section(path));
        req.setAttribute("site", app.settings().all());
        req.setAttribute("verdictLabels", Verdict.labels());
        req.setAttribute(CSRF_TOKEN, session.getAttribute(CSRF_TOKEN));
        if (user != null) {
            req.setAttribute(Ctx.USER, user);
            req.setAttribute("unreadMessages", app.messages().countUnread(user.getId()));
            touchLastSeen(app, user);
        }
        chain.doFilter(req, resp);
    }

    /** Records activity at most once every few minutes, to avoid a write on every request. */
    private static void touchLastSeen(AppContext app, User user) {
        LocalDateTime now = Times.now();
        if (user.getLastSeenAt() == null || user.getLastSeenAt().plusMinutes(LAST_SEEN_MINUTES).isBefore(now)) {
            app.users().touchLastSeen(user.getId(), now);
            user.setLastSeenAt(now);
        }
    }

    /** Which item of the top navigation the path belongs to. */
    private static String section(String path) {
        String first = path.length() > 1 ? path.substring(1).split("/", 2)[0] : "";
        return switch (first) {
            case "admin", "instructor", "student", "studio" -> "dashboard";
            case "learn" -> "learn";
            case "problems" -> "problems";
            case "challenges" -> "challenges";
            case "leaderboard" -> "leaderboard";
            case "messages" -> "messages";
            default -> "";
        };
    }
}
