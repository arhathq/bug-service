package bugapp.mail;

/**
 * Exception that thrown during mail sending
 *
 * @author Alexander Kuleshov
 */
public class MailException extends RuntimeException {
    public MailException(String message) {
        super(message);
    }

    public MailException(String message, Throwable cause) {
        super(message, cause);
    }
}
