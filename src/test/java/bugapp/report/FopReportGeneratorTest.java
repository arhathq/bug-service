package bugapp.report;

import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

/**
 * @author Alexander Kuleshov
 */
public class FopReportGeneratorTest extends TestCase {
    public void testGeneratePdfReport() throws URISyntaxException, IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        String reportXml = IOUtils.toString(classLoader.getResourceAsStream("bug-report.xml"));

        InputStream data = new ByteArrayInputStream(reportXml.getBytes(Charset.forName("UTF-8")));
        InputStream template = getClass().getClassLoader().getResourceAsStream("report.xsl");

        FopReportGenerator reportGenerator = new FopReportGenerator(new URI("fop1.xconf"));
        byte[] report = reportGenerator.generate(data, template);

//        String path = System.getProperty("java.io.tmpdir") + File.separator + "test_report.pdf";
//        OutputStream outputStream =  new FileOutputStream(new File(path));
//        outputStream.write(report);
//        outputStream.close();
    }

    public void testFailureCreateReportGenerator() throws URISyntaxException {
        try {
            new FopReportGenerator(null);
            fail();
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    public void testFailureGeneratePdfReport() throws URISyntaxException {
        FopReportGenerator reportGenerator = new FopReportGenerator(new URI("fop1.xconf"));

        InputStream data = new ByteArrayInputStream("<reports></reports>".getBytes(Charset.forName("UTF-8")));
        InputStream template = getClass().getClassLoader().getResourceAsStream("report.xsl");
        try {
            reportGenerator.generate(data, template);
            fail();
        } catch (Exception e) {
            assertTrue(true);
        }
    }
}
