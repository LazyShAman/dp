import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ScatterPlot {

    public static void main(String[] args) {
        // Ввод seed с консоли
        List<Integer> seed = readSeed();

        // Ввод taps с консоли
        List<Integer> taps = readTaps();

        LFSRGenerator generator = new LFSRGenerator(seed, taps);

        int sequenceLength = 32;
        List<Integer> bitSequence = generator.generateSequence(sequenceLength);

        DefaultXYDataset dataset = new DefaultXYDataset();
        double[][] data = new double[2][bitSequence.size()];
        for (int i = 0; i < bitSequence.size(); i++) {
            data[0][i] = i;
            data[1][i] = bitSequence.get(i);
        }
        dataset.addSeries("Bit Sequence", data);

        JFreeChart chart = ChartFactory.createScatterPlot(
                "Bit Sequence",
                "Bit Number",
                "Bit Value",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                false,
                false
        );

        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesLinesVisible(0, false);
        plot.setRenderer(renderer);

        ChartFrame frame = new ChartFrame("Bit Sequence", chart);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        // Шифрование изображения tux.bmp
        String inputFilePath = "tux.bmp";
        String outputFilePath = "encrypted_tux.bmp";
        String command = "openssl enc -aes-256-cbc -salt -in " + inputFilePath + " -out " + outputFilePath;
        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static List<Integer> readSeed() {
        List<Integer> seed = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Enter seed (comma-separated values): ");
            String input = reader.readLine();
            String[] values = input.split(",");
            for (String value : values) {
                seed.add(Integer.parseInt(value.trim()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return seed;
    }

    private static List<Integer> readTaps() {
        List<Integer> taps = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Enter taps (comma-separated values): ");
            String input = reader.readLine();
            String[] values = input.split(",");
            for (String value : values) {
                taps.add(Integer.parseInt(value.trim()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return taps;
    }
}