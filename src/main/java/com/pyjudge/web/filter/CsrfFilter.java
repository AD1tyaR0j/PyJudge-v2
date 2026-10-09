package com.pyjudge.web.filter;

import com.pyjudge.web.BaseServlet;
import com.pyjudge.web.Json;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * Cross-site request forgery protection: every POST must carry the token stored in the session.
 * Forms send it as the hidden {@code csrf_token} field; the JSON API sends an {@code X-CSRF-Token} header.
 */
public class CsrfFilter extends HttpFilter {
    private static final long serialVersionUID = 1L;
    private static final String MESSAGE = "Invalid or missing CSRF token. Reload the page and try again.";

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        if (!"POST".equals(req.getMethod())) {
            chain.doFilter(req, resp);
            return;
        }
        boolean api = BaseServlet.appPath(req).startsWith("/api/");
        String sent;
        if (api) {
            String type = req.getContentType();
            if (type == null || !type.toLowerCase().startsWith("application/json")) {
                Json.write(resp, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, Map.of("error", "Send JSON."));
                return;
            }
            sent = req.getHeader("X-CSRF-Token");
        } else {
            sent = req.getParameter("csrf_token");
        }

        HttpSession session = req.getSession(false);
        Object expected = session == null ? null : session.getAttribute(SessionFilter.CSRF_TOKEN);
        if (sent == null || !(expected instanceof String token) || !constantTimeEquals(sent, token)) {
            if (api) {
                Json.write(resp, HttpServletResponse.SC_BAD_REQUEST, Map.of("error", MESSAGE));
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, MESSAGE);
            }
            return;
        }
        chain.doFilter(req, resp);
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
