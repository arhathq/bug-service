package bugapp.mail;

import junit.framework.TestCase;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.mail.Message;
import javax.mail.Transport;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Properties;

import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.suppress;

/**
 * @author Alexander Kuleshov
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Transport.class)
public class MailServiceTest extends TestCase {
    private Properties mailProps = new Properties();
    private String mailUsername = "username";
    private String mailPassword = "password";

    private String from = "noreply@localhost.localdomain";
    private String[] to = new String[] {"user1@localhost.localdomain"};

    private String encoding = "UTF-8";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        suppress(method(Transport.class, "send", Message.class));
    }

    public void testSendTextMessage() {
        String[] cc = new String[] {"user2@localhost.localdomain", "user3@localhost.localdomain"};
        String[] bcc = new String[] {"manager@localhost.localdomain"};
        MailMessage message = new MailMessage("1", from, "Text message", to, cc, bcc,
                "admin@localhost.localdomain", "Text body", null, encoding, new Attachment[0]);
        MailService mailService = new MailServiceImpl(mailUsername, mailPassword, mailProps);

        mailService.sendMessage(message);
    }

    public void testSendHtmlMessage() {
        MailMessage message = new MailMessage("1", from, "Html message", to, new String[0], new String[0],
                null, null, "<p>Text body</p>", encoding, new Attachment[0]);
        MailService mailService = new MailServiceImpl(mailUsername, mailPassword, mailProps);

        mailService.sendMessage(message);
    }

    public void testSendMessageWithAttachment() {
        InputStream data = new ByteArrayInputStream("<report></report>".getBytes(Charset.forName("UTF-8")));
        Attachment attachment = new Attachment("Xml document", "text/xml", data);
        MailMessage message = new MailMessage("1", from, "Message with attachment", to, new String[0], new String[0],
                null, "<p>Attachment included</p>", null, encoding, new Attachment[] {attachment});
        MailService mailService = new MailServiceImpl(mailProps);

        mailService.sendMessage(message);
    }

    public void testThrowingMailException() {
        MailMessage message = new MailMessage("1", from, "Text message", to, null, null,
                "admin@localhost.localdomain", "Text body", null, encoding, new Attachment[0]);
        MailService mailService = new MailServiceImpl(mailProps);

        try {
            mailService.sendMessage(message);
            fail();
        } catch (Exception e) {
            assertTrue(true);
        }

    }
}
