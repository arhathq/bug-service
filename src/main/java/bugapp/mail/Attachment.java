package bugapp.mail;

import java.io.InputStream;

/**
 * Class that represents a mail attachment
 */
public class Attachment {

    private String name;
    private String mimeType;
    private InputStream data;

    /**
     * Constructor that creates attachment with name, mime type and data from input stream
     */
    public Attachment(String name, String mimeType, InputStream data) {
        this.name = name;
        this.mimeType = mimeType;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public InputStream getData() {
        return data;
    }
}
