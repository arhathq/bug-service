package bugapp.mail;

/**
 * Interface that represent mail service
 *
 * @author Alexander Kuleshov
 */
public interface MailService {
    /**
     * Method sends mail message
     */
    void sendMessage(MailMessage message);

}
