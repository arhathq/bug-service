package bugapp.report;

import org.apache.commons.codec.binary.Base64;
import org.jfree.chart.ChartColor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.encoders.EncoderUtil;
import org.jfree.chart.encoders.ImageFormat;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.*;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Class that contains different methods of chart generations
 *
 * @author Alexander Kuleshov
 */
public class ChartGenerator {

    private static final String IMAGE_FORMAT = ImageFormat.JPEG;

    /**
     * Create base64 representation of chart for Bugs Out Sla
     */
    public static String generateBase64OutSlaBugs(CategoryDataset dataset) {
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

    /**
     * Create base64 representation of chart for Sla Achievement Trend
     */
    public static String generateBase64SlaAchievementTrend(CategoryDataset dataset) {
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
    /**
     * Create base64 representation of chart for Opened P1,P2 bugs
     */
    public static String generateBase64OpenHighPriorityBugs(CategoryDataset dataset) {
        JFreeChart chart = ChartFactory.createStackedBarChart3D(
                "Production, Open, P1/P2 by Week",
                "", "Bug Count",
                dataset, PlotOrientation.VERTICAL,
                true, true, false
        );

        CategoryPlot plot = (CategoryPlot) chart.getPlot();

        plot.setRenderer(new StackedBarRenderer3D(24, 24));
        BarRenderer3D renderer = (BarRenderer3D) plot.getRenderer();

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
    /**
     * Create chart for bugs during the last 15 weeks
     */
    public static byte[] generateBugsFromLast15Weeks1(CategoryDataset dataset) {
        JFreeChart chart = ChartFactory.createStackedBarChart(
                "Prod Support Bugs - Last 15 Weeks",
                "", "Bug Count",
                dataset, PlotOrientation.VERTICAL,
                true, true, false
        );

        LegendTitle legend = chart.getLegend();
        legend.setPosition(RectangleEdge.RIGHT);

        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.setForegroundAlpha(0.8f);

        plot.setNoDataMessage("No data to display");

        final CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setLowerMargin(0.0);
        domainAxis.setUpperMargin(0.0);
        domainAxis.setTickLabelsVisible(false);

        // change the auto tick unit selection to integer units only...
        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, ChartColor.DARK_GRAY);
        renderer.setSeriesPaint(1, ChartColor.LIGHT_BLUE);
        renderer.setSeriesPaint(2, ChartColor.LIGHT_RED);

        renderer.setBaseItemLabelsVisible(true);

        return chartToBytes(1024, 600, chart);
    }

    /**
     * Create base64 representation for chart with bugs during the last 15 weeks
     */
    public static String generateBase64BugsFromLast15Weeks1(CategoryDataset dataset) {
        return toBase64(generateBugsFromLast15Weeks1(dataset));
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
    /**
     * Create base64 representation for chart New Vs Resolved Bugs
     */
    public static String generateBase64NewVsResolvedBugs(CategoryDataset dataset) {
        JFreeChart chart = ChartFactory.createBarChart("", "", "", dataset, PlotOrientation.VERTICAL, true, true, false);

        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.getRangeAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, ChartColor.VERY_LIGHT_RED);
        renderer.setSeriesPaint(1, ChartColor.VERY_LIGHT_GREEN);
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

        return toBase64(chartToBytes(800, 600, chart));
    }

    private static byte[] chartToBytes(int width, int height, JFreeChart chart) throws ReportGenerationException {
        try {
            BufferedImage image = chart.createBufferedImage(width, height, BufferedImage.TYPE_INT_RGB, null);
            return EncoderUtil.encode(image, IMAGE_FORMAT);
        } catch (Exception e) {
            throw new ReportGenerationException("Conversion error", e);
        }
    }

    private static String toBase64(byte[] bytes) {
        return Base64.encodeBase64String(bytes);
    }

}