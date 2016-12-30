package bugapp.report;

import junit.framework.TestCase;
import org.jfree.data.category.DefaultCategoryDataset;

import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class ChartGeneratorTests extends TestCase {
    public void testChartBugsFromLast15Weeks() throws Exception {

        String open = "Open";
        String closed = "Closed";
        String invalid = "Invalid";

        int[] openBugs =    new int[] {11, 13, 7, 24, 9, 17, 8, 9, 10, 7, 6, 10, 8, 7, 11};
        int[] closedBugs =  new int[] { 7,  2, 5, 21, 5, 14, 7, 9,  6, 6, 3,  8, 6, 6,  9};
        int[] invalidBugs = new int[] { 0,  1, 0,  2, 2,  2, 1, 0,  14, 11, 2,  1, 1, 0,  2};
        List<String> weeks = Arrays.asList("2015-25", "2015-26", "2015-27", "2015-28", "2015-29", "2015-30", "2015-31", "2015-32", "2015-33", "2015-34", "2015-35", "2015-36", "2015-37", "2015-38", "2015-39");

        DefaultCategoryDataset bugsFromLast15Weeks = new DefaultCategoryDataset();

        for(int i = 0; i < weeks.size(); i++) {
            bugsFromLast15Weeks.addValue(openBugs[i], open, weeks.get(i));
            bugsFromLast15Weeks.addValue(closedBugs[i], closed, weeks.get(i));
            bugsFromLast15Weeks.addValue(invalidBugs[i], invalid, weeks.get(i));
        }

        byte[] chart = ChartGenerator.generateBugsFromLast15Weeks(bugsFromLast15Weeks);
        FileOutputStream fos = new FileOutputStream("15LastWeeks.jpeg");
        fos.write(chart);
        fos.close();
    }
}
