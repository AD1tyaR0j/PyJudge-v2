package com.pyjudge.web.filter;

import com.pyjudge.model.User;
import com.pyjudge.web.BaseServlet;
import com.pyjudge.web.Ctx;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Role-based access to whole areas of the site, enforced before any servlet runs:
 * {@code /admin} is for admins; {@code /instructor} and {@code /studio} are for instructors and admins.
 * Guests are sent to the login page; logged-in users without the role get 403.
 */
public class AccessFilter extends HttpFilter {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        String path = BaseServlet.appPath(req);
        boolean adminArea = under(path, "/admin");
        boolean authorArea = under(path, "/instructor") || under(path, "/studio");
        if (!adminArea && !authorArea) {
            chain.doFilter(req, resp);
            return;
        }

        User user = (User) req.getAttribute(Ctx.USER);
        if (user == null) {
            String query = req.getQueryString();
            String next = path + (query == null ? "" : "?" + query);
            resp.sendRedirect(req.getContextPath() + "/login?next=" + URLEncoder.encode(next, StandardCharsets.UTF_8));
        } else if (adminArea ? !user.isAdmin() : !user.isAuthor()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "You don't have access to that page.");
        } else {
            chain.doFilter(req, resp);
        }
    }

    private static boolean under(String path, String area) {
        return path.equals(area) || path.startsWith(area + "/");
    }
}
