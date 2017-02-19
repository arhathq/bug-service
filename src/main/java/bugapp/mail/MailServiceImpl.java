package bugapp.mail;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import java.util.Date;
import java.util.Properties;

/**
 *
 */
public class MailServiceImpl implements MailService {

    private String username;
    private String password;
    private Properties mailProps = new Properties();

    public MailServiceImpl(String username, String password, Properties mailProps) {
        this.username = username;
        this.password = password;
        this.mailProps = mailProps;
    }

    public void sendMessage(MailMessage message) throws MailException {

        try {
            // Create a single message for every receiver
            for (String to : message.getTo()) {

                MimeMessage msg = createMimeMessage();
                msg.setFrom(new InternetAddress(message.getFrom(), message.getReplyTo()));
                msg.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
                msg.setSentDate(new Date());
                msg.setSubject(MimeUtility.encodeText(message.getSubject(), message.getEncoding(), "Q"));
                if (message.getReplyTo() != null) {
                    msg.setReplyTo(new InternetAddress[]{new InternetAddress(message.getReplyTo())});
                }

                // Is it a multipart message
                String htmlMessage = message.getHtmlMessage();
                Attachment[] attachments = message.getAttachments();

                boolean isMultipart = htmlMessage != null || attachments.length > 0;

                if (isMultipart) {
                    MimeMultipart multipart = new MimeMultipart();

                    // First part is always the text message itself
                    if (message.getText() != null && !message.getText().isEmpty()) {
                        MimeBodyPart mbpMessageText = new MimeBodyPart();
                        mbpMessageText.setText(message.getText(), message.getEncoding());
                        multipart.addBodyPart(mbpMessageText);
                    }

                    // Add Html message
                    if (htmlMessage != null && !htmlMessage.isEmpty()) {
                        MimeBodyPart mbpHtmlMessage = new MimeBodyPart();
                        mbpHtmlMessage.setContent(htmlMessage, "text/html; charset=" + message.getEncoding());
                        multipart.addBodyPart(mbpHtmlMessage);
                    }

                    // Add attachments
                    if (attachments.length > 0) {
                        for (Attachment attachment : attachments) {
                            MimeBodyPart mbpAttachment = new MimeBodyPart();
                            ByteArrayDataSource ds = new ByteArrayDataSource(attachment.getData(), attachment.getMimeType());
                            ds.setName(attachment.getName());
                            mbpAttachment.setDataHandler(new DataHandler(ds));
                            mbpAttachment.setFileName(MimeUtility.encodeText(ds.getName(), message.getEncoding(), "Q"));
                            multipart.addBodyPart(mbpAttachment);
                        }
                    }

                    msg.setContent(multipart);

                } else {
                    msg.setText(message.getText(), message.getEncoding());
                }
                sendMimeMessage(msg);
            }
        } catch (Exception e) {
            throw new MailException("Couldn't send a mail, the cause is", e);
        }

    }

    private MimeMessage createMimeMessage() {
        Session session = Session.getInstance(mailProps,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        return new MimeMessage(session);
    }

    private void sendMimeMessage(MimeMessage message) throws MessagingException {
        Transport.send(message);
    }
}
