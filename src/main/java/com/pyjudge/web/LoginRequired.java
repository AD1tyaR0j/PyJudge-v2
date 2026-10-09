package com.pyjudge.web;

/** Thrown when a page needs a logged-in user; the visitor is sent to the login form and back afterwards. */
public class LoginRequired extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public LoginRequired() {
        super("Login required");
    }
}
