package bugapp.mail;

/**
 *
 */
public interface MailService {

    void sendMessage(MailMessage message) throws MailException;

}
