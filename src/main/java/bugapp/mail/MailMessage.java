package bugapp.mail;

/**
 *
 */
public class MailMessage {
    private String from;
    private String subject;
    private String[] to;
    private String replyTo;
    private String text;
    private String htmlMessage;
    private String encoding;
    private Attachment[] attachments;

    public MailMessage(String from, String subject, String[] to, String replyTo, String text, String htmlMessage, String encoding, Attachment[] attachments) {
        this.from = from;
        this.subject = subject;
        this.to = to;
        this.replyTo = replyTo;
        this.text = text;
        this.htmlMessage = htmlMessage;
        this.encoding = encoding;
        this.attachments = attachments;
    }

    public String getFrom() {
        return from;
    }

    public String getSubject() {
        return subject;
    }

    public String[] getTo() {
        return to;
    }

    public String getReplyTo() {
        return replyTo;
    }

    public String getText() {
        return text;
    }

    public String getHtmlMessage() {
        return htmlMessage;
    }

    public String getEncoding() {
        return encoding;
    }

    public Attachment[] getAttachments() {
        return attachments;
    }
}
