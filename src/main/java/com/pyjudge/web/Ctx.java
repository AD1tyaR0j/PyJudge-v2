package com.pyjudge.web;

import com.pyjudge.AppContext;
import com.pyjudge.model.User;
import com.pyjudge.service.Form;
import com.pyjudge.service.Outcome;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Everything a request handler needs: the request and response, the path variables of the matched
 * route, the logged-in user, and shortcuts for rendering a JSP, redirecting and flash messages.
 */
public final class Ctx {
    /** Request attribute holding the logged-in {@link User} (absent for guests). */
    public static final String USER = "currentUser";
    /** Session attribute holding the id of the logged-in user. */
    public static final String SESSION_USER_ID = "userId";
    private static final String SESSION_FLASHES = "flashes";

    private final HttpServletRequest req;
    private final HttpServletResponse resp;
    private final AppContext app;
    private final Map<String, String> pathVars;

    public Ctx(HttpServletRequest req, HttpServletResponse resp, AppContext app, Map<String, String> pathVars) {
        this.req = req;
        this.resp = resp;
        this.app = app;
        this.pathVars = pathVars;
    }

    public HttpServletRequest request() { return req; }
    public HttpServletResponse response() { return resp; }
    public AppContext app() { return app; }

    // ------------------------------------------------------------ who is asking

    /** The logged-in user, or null for a guest. */
    public User user() {
        return (User) req.getAttribute(USER);
    }

    /** The logged-in user; guests are sent to the login page. */
    public User requireUser() {
        User user = user();
        if (user == null) {
            throw new LoginRequired();
        }
        return user;
    }

    /** The logged-in instructor or admin; everyone else gets 403. */
    public User requireAuthor() {
        User user = requireUser();
        if (!user.isAuthor()) {
            throw HttpError.forbidden();
        }
        return user;
    }

    // ------------------------------------------------------------ input

    /** A path variable such as {@code {slug}}. */
    public String path(String name) {
        return pathVars.get(name);
    }

    /** A numeric path variable such as {@code {id}}; anything else is a 404. */
    public int pathInt(String name) {
        try {
            return Integer.parseInt(pathVars.get(name));
        } catch (NumberFormatException e) {
            throw HttpError.notFound();
        }
    }

    /** A request parameter, trimmed; an empty string when absent. */
    public String param(String name) {
        String value = req.getParameter(name);
        return value == null ? "" : value.strip();
    }

    /** A numeric request parameter, or null when it is absent or not a number. */
    public Integer intParam(String name) {
        try {
            return Integer.valueOf(param(name));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** The {@code page} parameter, at least 1. */
    public int page() {
        Integer page = intParam("page");
        return page == null || page < 1 ? 1 : page;
    }

    public Form form() {
        return Form.of(req.getParameterMap());
    }

    // ------------------------------------------------------------ output

    /** Makes a value available to the JSP as {@code ${name}}. */
    public Ctx set(String name, Object value) {
        req.setAttribute(name, value);
        return this;
    }

    /** Forwards to {@code /WEB-INF/views/<view>.jsp}; pending flash messages are shown on this page. */
    public void render(String view) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            Object flashes = session.getAttribute(SESSION_FLASHES);
            if (flashes != null) {
                req.setAttribute("flashes", flashes);
                session.removeAttribute(SESSION_FLASHES);
            }
        }
        resp.setContentType("text/html;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-store"); // pages are per-user; never serve one from a shared cache
        req.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(req, resp);
    }

    /** Redirects to a path inside the application, e.g. {@code /admin/users}. */
    public void redirect(String appPath) throws IOException {
        resp.sendRedirect(resp.encodeRedirectURL(req.getContextPath() + appPath));
    }

    /**
     * Redirects back to the page the form was submitted from when that page is part of this
     * application, otherwise to {@code fallback}.
     */
    public void redirectBack(String fallback) throws IOException {
        String referer = req.getHeader("Referer");
        String target = fallback;
        if (referer != null) {
            try {
                URI uri = new URI(referer);
                String context = req.getContextPath();
                boolean sameHost = uri.getHost() == null || uri.getHost().equalsIgnoreCase(req.getServerName());
                String path = uri.getRawPath();
                if (sameHost && path != null && path.startsWith(context + "/")) {
                    target = path.substring(context.length()) + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
                }
            } catch (URISyntaxException e) {
                // fall back
            }
        }
        redirect(target);
    }

    public void json(int status, Object body) throws IOException {
        Json.write(resp, status, body);
    }

    // ------------------------------------------------------------ flash messages

    /** Queues a one-time message for the next rendered page. Categories: success, error, info. */
    public void flash(String category, String message) {
        HttpSession session = req.getSession();
        @SuppressWarnings("unchecked")
        List<String[]> flashes = (List<String[]>) session.getAttribute(SESSION_FLASHES);
        if (flashes == null) {
            flashes = new ArrayList<>();
        }
        flashes.add(new String[] {category, message});
        session.setAttribute(SESSION_FLASHES, flashes); // set again so clustered sessions notice the change
    }

    public void flashErrors(List<String> errors) {
        errors.forEach(e -> flash("error", e));
    }

    /** Flashes an outcome's errors, or its success message. Returns whether it succeeded. */
    public boolean flash(Outcome outcome, String successCategory) {
        if (!outcome.succeeded()) {
            flashErrors(outcome.errors());
            return false;
        }
        if (outcome.message() != null) {
            flash(successCategory, outcome.message());
        }
        return true;
    }
}
