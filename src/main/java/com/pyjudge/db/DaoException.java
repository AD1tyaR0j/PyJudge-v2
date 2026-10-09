package com.pyjudge.db;

/** Unchecked wrapper for {@link java.sql.SQLException}, so callers are not forced to handle JDBC details. */
public class DaoException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DaoException(String message) {
        super(message);
    }

    public DaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
