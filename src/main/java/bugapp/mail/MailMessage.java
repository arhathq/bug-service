package bugapp.mail;

/**
 * Class that represents a mail message
 */
public class MailMessage {
    private String id;
    private String from;
    private String subject;
    private String[] to;
    private String[] cc;
    private String[] bcc;
    private String replyTo;
    private String text;
    private String htmlMessage;
    private String encoding;
    private Attachment[] attachments;

    /**
     * Constructor that creates mail message
     */
    public MailMessage(String id, String from, String subject, String[] to, String[] cc, String[] bcc, String replyTo, String text, String htmlMessage, String encoding, Attachment[] attachments) {
        this.id = id;
        this.from = from;
        this.subject = subject;
        this.to = to;
        this.cc = cc;
        this.bcc = bcc;
        this.replyTo = replyTo;
        this.text = text;
        this.htmlMessage = htmlMessage;
        this.encoding = encoding;
        this.attachments = attachments;
    }

    public String getId() {
        return id;
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

    public String[] getCc() {
        return cc;
    }

    public String[] getBcc() {
        return bcc;
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
