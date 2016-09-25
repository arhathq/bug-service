package bugapp.report;

import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URI;

/**
 * @author Alexander Kuleshov
 */
public class FopReportGenerator {

    private String outputFormat = MimeConstants.MIME_PDF;

    private FopFactory fopFactory;
    private TransformerFactory transformerFactory;

    public FopReportGenerator(URI configUri) {
        try {
            fopFactory = FopFactory.newInstance(configUri);
            transformerFactory = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null);
        } catch (Exception e) {
            throw new RuntimeException("Unable to create Fop generator", e);
        }
    }

    public byte[] generate(InputStream data, InputStream template) throws Exception {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            Fop fop = fopFactory.newFop(outputFormat, os);
            Transformer transformer = transformerFactory.newTransformer(new StreamSource(template));
            Source src = new StreamSource(data);
            Result res = new SAXResult(fop.getDefaultHandler());
            transformer.transform(src, res);
            return os.toByteArray();
        }
    }

    public static void main(String[] args) throws Exception {
        FopReportGenerator reportGenerator = new FopReportGenerator(new URI("fop1.xconf"));

        InputStream data = FopReportGenerator.class.getClassLoader().getResourceAsStream("bug-report.xml");
        InputStream template = FopReportGenerator.class.getClassLoader().getResourceAsStream("bug-report.xsl");

        byte[] output = reportGenerator.generate(data, template);
        OutputStream os = new FileOutputStream("d:/report.pdf");
        os.write(output);
    }
}
