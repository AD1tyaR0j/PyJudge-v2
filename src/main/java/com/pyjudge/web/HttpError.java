package com.pyjudge.web;

import jakarta.servlet.http.HttpServletResponse;

/** Thrown by a request handler to stop with an HTTP error status (404, 403, 400...). */
public class HttpError extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final int status;

    public HttpError(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public static HttpError notFound() {
        return new HttpError(HttpServletResponse.SC_NOT_FOUND, "That page doesn't exist.");
    }

    public static HttpError forbidden() {
        return new HttpError(HttpServletResponse.SC_FORBIDDEN, "You don't have access to that page.");
    }

    public static HttpError badRequest(String message) {
        return new HttpError(HttpServletResponse.SC_BAD_REQUEST, message);
    }
}
