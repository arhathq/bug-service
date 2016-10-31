package bugapp.report;

import org.apache.commons.codec.binary.Base64;
import org.jfree.chart.ChartColor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.encoders.EncoderUtil;
import org.jfree.chart.encoders.ImageFormat;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.AreaRendererEndType;
import org.jfree.chart.renderer.category.*;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * @author Alexander Kuleshov
 */
public class ChartGenerator {

    private static final String imageFormat = ImageFormat.JPEG;


    public static String generateBase64OutSlaBugs(CategoryDataset dataset) throws Exception {
        JFreeChart chart = ChartFactory.createBarChart(
                "Production, Bugs out SLA, P1/P2 by Week",
                "Week Year", "Bug Count",
                dataset, PlotOrientation.VERTICAL,
                true, true, false
        );

        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.getRangeAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, ChartColor.VERY_LIGHT_RED);
        renderer.setSeriesPaint(1, ChartColor.VERY_LIGHT_BLUE);
        renderer.setMaximumBarWidth(3.0);
        renderer.setItemMargin(0.0);

        renderer.setBarPainter(new StandardBarPainter());

        renderer.setBaseItemLabelsVisible(true);
        renderer.setBaseItemLabelGenerator(new StandardCategoryItemLabelGenerator());

        renderer.setSeriesItemLabelFont(0, new java.awt.Font("SansSerif", Font.BOLD, 14));
        renderer.setSeriesItemLabelFont(1, new java.awt.Font("SansSerif", Font.BOLD, 14));

        renderer.setSeriesItemLabelPaint(0, Color.black);
        renderer.setSeriesItemLabelPaint(1, Color.black);

        renderer.setSeriesPositiveItemLabelPosition(0, new ItemLabelPosition(ItemLabelAnchor.CENTER, TextAnchor.TOP_CENTER, TextAnchor.TOP_CENTER, 0));
        renderer.setSeriesPositiveItemLabelPosition(1, new ItemLabelPosition(ItemLabelAnchor.CENTER, TextAnchor.TOP_CENTER, TextAnchor.TOP_CENTER, 0));

        return toBase64(chartToBytes(640, 480, chart));
    }

    public static String generateBase64SlaAchievementTrend(CategoryDataset dataset) throws Exception {
        JFreeChart chart = ChartFactory.createLineChart(
                "P1/P2 SLA % achievement trend",  // title
                "Year week",             // x-axis label
                "SLA, %",                // y-axis label
                dataset,            // data
                PlotOrientation.VERTICAL,
                true,               // create legend?
                true,               // generate tooltips?
                false               // generate URLs?
        );

        chart.setBackgroundPaint(Color.white);
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);

        CategoryItemRenderer r = plot.getRenderer();
        if (r instanceof LineAndShapeRenderer) {
            LineAndShapeRenderer renderer = (LineAndShapeRenderer) r;
            renderer.setDrawOutlines(true);
            renderer.setUseFillPaint(true);
            renderer.setBaseShapesVisible(true);
            renderer.setBaseShapesFilled(true);
            renderer.setBaseItemLabelsVisible(true);
            renderer.setSeriesItemLabelPaint(0, Color.red);
            renderer.setSeriesItemLabelPaint(1, Color.blue);
        }

        return toBase64(chartToBytes(640, 480, chart));
    }

    /*
        <P1>
            <2015-38>1</2015-38>
            <2015-39>0</2015-39>
            <2015-40>1</2015-40>
        </P1>
        <P2>
            <2015-38>1</2015-38>
            <2015-39>2</2015-39>
            <2015-40>8</2015-40>
        </P2>
    */
    public static String generateBase64OpenHighPriorityBugs(CategoryDataset dataset) throws Exception {
        JFreeChart chart = ChartFactory.createStackedBarChart3D(
                "Production, Open, P1/P2 by Week",
                "", "Bug Count",
                dataset, PlotOrientation.VERTICAL,
                true, true, false
        );

        return toBase64(chartToBytes(640, 480, chart));
    }

    /*
        <Open>
            <2015-25></2015-25>
            <2015-26></2015-26>
            <2015-27></2015-27>
            <2015-28></2015-28>
            <2015-29></2015-29>
            <2015-30></2015-30>
            <2015-31></2015-31>
            <2015-32></2015-32>
            <2015-33></2015-33>
            <2015-34></2015-34>
            <2015-35></2015-35>
            <2015-36></2015-36>
            <2015-37></2015-37>
            <2015-38></2015-38>
            <2015-39></2015-39>
            <2015-40></2015-40>
        </Open>
        <Closed>
            <2015-25></2015-25>
            <2015-26></2015-26>
            <2015-27></2015-27>
            <2015-28></2015-28>
            <2015-29></2015-29>
            <2015-30></2015-30>
            <2015-31></2015-31>
            <2015-32></2015-32>
            <2015-33></2015-33>
            <2015-34></2015-34>
            <2015-35></2015-35>
            <2015-36></2015-36>
            <2015-37></2015-37>
            <2015-38></2015-38>
            <2015-39></2015-39>
            <2015-40></2015-40>
        </Closed>
        <Invalid>
            <2015-25></2015-25>
            <2015-26></2015-26>
            <2015-27></2015-27>
            <2015-28></2015-28>
            <2015-29></2015-29>
            <2015-30></2015-30>
            <2015-31></2015-31>
            <2015-32></2015-32>
            <2015-33></2015-33>
            <2015-34></2015-34>
            <2015-35></2015-35>
            <2015-36></2015-36>
            <2015-37></2015-37>
            <2015-38></2015-38>
            <2015-39></2015-39>
            <2015-40></2015-40>
        </Invalid>
     */
    public String generateBase64BugsFromLast15Weeks(CategoryDataset dataset) throws Exception {
        JFreeChart chart = ChartFactory.createAreaChart(
                "Prod Support Bugs - Last 15 Weeks",
                "", "Bug Count",
                dataset, PlotOrientation.VERTICAL,
                true, true, false
        );

        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        AreaRenderer renderer = (AreaRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, Color.cyan);
        renderer.setSeriesPaint(1, Color.white);
        renderer.setSeriesPaint(2, Color.pink);
        renderer.setEndType(AreaRendererEndType.TRUNCATE);

        return toBase64(chartToBytes(640, 480, chart));
    }

    /*
        <New>
            <2015-06-01>1</2015-06-01>
            <2015-06-02>3</2015-06-02>
            <2015-06-03>6</2015-06-03>
        </New>
        <Resolved>
            <2015-06-01>2</2015-06-01>
            <2015-06-02>3</2015-06-02>
            <2015-06-03>4</2015-06-03>
        </Resolved>
     */
    public String generateBase64NewVsResolvedBugs(CategoryDataset dataset) throws Exception {
        JFreeChart chart = ChartFactory.createBarChart("", "", "", dataset, PlotOrientation.VERTICAL, true, true, false);

        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, Color.red);
        renderer.setSeriesPaint(1, Color.green);
        renderer.setMaximumBarWidth(3.0);
        renderer.setItemMargin(0.0);

        return toBase64(chartToBytes(640, 480, chart));
    }

    private void saveChart(int width, int height, String filename, JFreeChart chart) throws Exception {
        File file = new File(filename);
        ChartUtilities.saveChartAsJPEG(file, chart, width, height);
    }

    private static byte[] chartToBytes(int width, int height, JFreeChart chart) throws Exception {
        BufferedImage image = chart.createBufferedImage(width, height, BufferedImage.TYPE_INT_RGB, null);
        return EncoderUtil.encode(image, imageFormat);
    }

    private static String toBase64(byte[] bytes) throws Exception {
        return Base64.encodeBase64String(bytes);
    }

    public static void main(String[] args)throws Exception {

        ChartGenerator chartGenerator = new ChartGenerator();

        String p1 = "P1";
        String p2 = "P2";

        String week1 = "2015-38";
        String week2 = "2015-39";
        String week3 = "2015-40";

        DefaultCategoryDataset openHighPriorityBugs = new DefaultCategoryDataset();

        openHighPriorityBugs.addValue(1.0, p1, week1);
        openHighPriorityBugs.addValue(0.0, p1, week2);
        openHighPriorityBugs.addValue(1.0, p1, week3);

        openHighPriorityBugs.addValue(1.0, p2, week1);
        openHighPriorityBugs.addValue(2.0, p2, week2);
        openHighPriorityBugs.addValue(8.0, p2, week3);

        System.out.println(generateBase64OpenHighPriorityBugs(openHighPriorityBugs));

        String open = "Open";
        String closed = "Closed";
        String invalid = "Invalid";

        int[] openBugs =    new int[] {11, 13, 7, 24, 9, 17, 8, 9, 10, 7, 6, 10, 8, 7, 11};
        int[] closedBugs =  new int[] { 7,  2, 5, 21, 5, 14, 7, 9,  6, 6, 3,  8, 6, 6,  9};
        int[] invalidBugs = new int[] { 0,  1, 0,  2, 2,  2, 1, 0,  4, 1, 2,  1, 1, 0,  2};
        List<String> weeks = Arrays.asList("2015-25", "2015-26", "2015-27", "2015-28", "2015-29", "2015-30", "2015-31", "2015-32", "2015-33", "2015-34", "2015-35", "2015-36", "2015-37", "2015-38", "2015-39");

        DefaultCategoryDataset bugsFromLast15Weeks = new DefaultCategoryDataset();

        for(int i = 0; i < weeks.size(); i++) {
            bugsFromLast15Weeks.addValue(openBugs[i], open, weeks.get(i));
            bugsFromLast15Weeks.addValue(closedBugs[i], closed, weeks.get(i));
            bugsFromLast15Weeks.addValue(invalidBugs[i], invalid, weeks.get(i));
        }
        System.out.println(chartGenerator.generateBase64BugsFromLast15Weeks(bugsFromLast15Weeks));

        String[] days = new String[] {"2015-06-01", "2015-06-02", "2015-06-03", "2015-06-04", "2015-06-05", "2015-06-06", "2015-06-07"};
        int[] newBugs = new int[] {11, 13, 7, 24, 9, 17, 8};
        int[] resolvedBugs = new int[] {7, 2, 5, 21, 5, 14, 7};

        DefaultCategoryDataset newVsResolved = new DefaultCategoryDataset();
        for(int i = 0; i < days.length; i++) {
            newVsResolved.addValue(newBugs[i], "New", days[i]);
            newVsResolved.addValue(resolvedBugs[i], "Resolved", days[i]);
        }

        System.out.println(chartGenerator.generateBase64NewVsResolvedBugs(newVsResolved));
    }
}