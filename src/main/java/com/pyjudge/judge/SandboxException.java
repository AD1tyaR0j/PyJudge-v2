package com.pyjudge.judge;

/** The sandbox itself failed (Python or Docker is missing, the temp directory is not writable...). */
public class SandboxException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SandboxException(String message, Throwable cause) {
        super(message, cause);
    }
}
