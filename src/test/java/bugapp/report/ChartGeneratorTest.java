package bugapp.report;

import junit.framework.TestCase;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


/**
 * @author Alexander Kuleshov
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ChartGenerator.class)
@PowerMockIgnore("javax.swing.*")
public class ChartGeneratorTest extends TestCase {
    public void testOutSlaBugsChart() {
        CategoryDataset dataset = new DefaultCategoryDataset();

        ChartGenerator.generateBase64OutSlaBugs(dataset);
    }

    public void testSlaAchievementTrend() {
        CategoryDataset dataset = new DefaultCategoryDataset();

        ChartGenerator.generateBase64SlaAchievementTrend(dataset);
    }

    public void testOpenHighPriorityBugs() {
        CategoryDataset dataset = new DefaultCategoryDataset();

        ChartGenerator.generateBase64OpenHighPriorityBugs(dataset);
    }

    public void testBugsFromLast15Weeks() {
        CategoryDataset dataset = new DefaultCategoryDataset();

        ChartGenerator.generateBase64BugsFromLast15Weeks1(dataset);
    }

    public void testNewVsResolvedBugs() {
        CategoryDataset dataset = new DefaultCategoryDataset();

        ChartGenerator.generateBase64NewVsResolvedBugs(dataset);
    }

    public void testErrorInPrivateMethodChartToBytes() throws Exception {
        PowerMockito.spy(ChartGenerator.class);
        try {
            PowerMockito.when(ChartGenerator.class, "chartToBytes", 1, 1, null);
            fail();
        } catch (Exception e) {
            assertTrue(true);
        }
    }
}
