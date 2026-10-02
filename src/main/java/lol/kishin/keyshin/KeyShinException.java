package lol.kishin.keyshin;

/**
 * The license could not be checked (network problem, server error, unreadable response).
 * This is different from the server saying the license is invalid.
 */
public class KeyShinException extends Exception {

    private static final long serialVersionUID = 1L;

    private final int statusCode;
    private final String serverError;

    public KeyShinException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.serverError = null;
    }

    public KeyShinException(String message, int statusCode, String serverError) {
        super(serverError == null ? message : message + ": " + serverError);
        this.statusCode = statusCode;
        this.serverError = serverError;
    }

    /** HTTP status from the server, or -1 if no response was received. */
    public int getStatusCode() {
        return statusCode;
    }

    /** The {@code error} field the server sent with a 400, or null. */
    public String getServerError() {
        return serverError;
    }
}
