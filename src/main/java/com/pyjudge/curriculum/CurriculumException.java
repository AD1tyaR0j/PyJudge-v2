package com.pyjudge.curriculum;

/** A course file is malformed (missing front matter, a quiz question without an answer...). */
public class CurriculumException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CurriculumException(String message) {
        super(message);
    }

    public CurriculumException(String message, Throwable cause) {
        super(message, cause);
    }
}
