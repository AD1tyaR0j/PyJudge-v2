package com.pyjudge.service;

import java.util.List;

/**
 * Result of a form action: either validation errors to show, or a success message.
 *
 * @param errors  empty when the action succeeded
 * @param message what to tell the user after a success (may be null)
 */
public record Outcome(List<String> errors, String message) {
    public static Outcome ok(String message) {
        return new Outcome(List.of(), message);
    }

    public static Outcome fail(String error) {
        return new Outcome(List.of(error), null);
    }

    public static Outcome fail(List<String> errors) {
        return new Outcome(List.copyOf(errors), null);
    }

    public boolean succeeded() {
        return errors.isEmpty();
    }
}
