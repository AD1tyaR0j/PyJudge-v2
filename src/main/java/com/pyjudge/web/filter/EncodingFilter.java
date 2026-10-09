package com.pyjudge.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Reads form posts and writes responses as UTF-8, so names, code and emoji survive the round trip. */
public class EncodingFilter extends HttpFilter {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        if (req.getCharacterEncoding() == null) {
            req.setCharacterEncoding("UTF-8");
        }
        resp.setCharacterEncoding("UTF-8");
        chain.doFilter(req, resp);
    }
}
