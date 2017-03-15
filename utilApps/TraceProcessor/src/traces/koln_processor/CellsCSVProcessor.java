/**
 * 2017-03-04
 *
 * @author xvas
 */
package traces.koln_processor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import utilities.random.RandomGenerator;

public class CellsCSVProcessor {

    public static void main(String[] args) {

        String csvRdFile = "D:\\xvas\\dev\\MECOMM\\bs.csv";

        List<double[]> csvAsList = new ArrayList();

        CellsCSVProcessor csvLoader = new CellsCSVProcessor();

        csvLoader.parseCSV(csvRdFile, csvAsList);

        // Set the max coverage area
        double d = 2500;// area will resemble a square d x d
        double maxArea = Math.pow(d, 2);

        for (int seed = 0; seed < 129; seed++) {
            RandomGenerator rnd = new RandomGenerator(seed); // pick random point in list

            int randStart = rnd.randIntInRange(0, csvAsList.size());

            // pick up first some random line from the trace to start the sampling.
            List<double[]> rndStartCSVLines = csvAsList.subList(randStart, csvAsList.size());

            boolean areaExided = false;

            double prevMinX = Double.MAX_VALUE;
            double minX = Double.MAX_VALUE;

            double prevMinY = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;

            double prevMaxX = 0;
            double maxX = 0;

            double prevMaxY = 0;
            double maxY = 0;

            double prevCurrArea = 0.0;
            double currArea = 0.0;

            List<double[]> selectedCSV = new ArrayList();

            do {

                FOR_subCSVLines:
                for (double[] line : rndStartCSVLines) {

                    // min X and min Y updated with each next line read
                    prevMinX = minX;
                    prevMinX = minX = Math.min(minX, line[1]);

                    prevMinY = minY;
                    prevMinY = minY = Math.min(minY, line[2]);

                    prevMaxX = maxX;
                    prevMaxX = maxX = Math.max(maxX, line[1]);

                    prevMaxY = maxY;
                    prevMaxY = maxY = Math.max(maxY, line[2]);

                    double distX = maxX - minX;
                    double distY = maxY - minY;

                    prevCurrArea = currArea;
                    currArea = distX * distY;

                    // max X and max Y updated with each next line read
                    // stop reading if exids max coverage area
                    areaExided = Math.abs(currArea - maxArea) < 0.05 * maxArea;
                    boolean areaOverExided = currArea - maxArea > 0.1 * maxArea;

                    if (!areaOverExided) {
                        selectedCSV.add(line);
                        if (areaExided) {
                            break FOR_subCSVLines;
                        }

//                    System.err.println("**** !areaOverExided ****");
//
//                    System.err.print("--------- added line " + Arrays.toString(line));
//                    System.err.println(" currArea as % of maxArea = " + ((int) currArea / maxArea * 10000) / 100.0);
//                    System.err.println(" maxArea " + maxArea);
//                    System.err.println(" maxX " + maxX);
//                    System.err.println(" maxY " + maxY);
//                    System.err.println(" minX " + minX);
//                    System.err.println(" minY " + minY);
//
//                    try {
//                        System.in.read();
//                    } catch (IOException ex) {
//                        Logger.getLogger(CellsCSVProcessor.class.getName()).log(Level.SEVERE, null, ex);
//                    }
                    } else {

//                    System.err.println("areaOverExided by line " + Arrays.toString(line));
//                    System.err.println(" currArea " + currArea);
//                    System.err.println(" maxArea " + maxArea);
//                    System.err.println(" maxX " + maxX);
//                    System.err.println(" maxY " + maxY);
//                    System.err.println(" minX " + minX);
//                    System.err.println(" minY " + minY);
                        currArea = prevCurrArea;
                        maxX = prevMaxX;
                        maxY = prevMaxY;
                        minX = prevMinX;
                        minY = prevMinY;

                        areaExided = false;

                    }
                }//FOR_subCSVLines

                if (!areaExided) {/* then reset and retry from a 
                        different starting point in 
                        the list of cells*/

                    rnd = new RandomGenerator(++seed);
                    randStart = rnd.randIntInRange(0, csvAsList.size());
                    rndStartCSVLines = csvAsList.subList(randStart, csvAsList.size());

                    prevMinX = minX = Double.MAX_VALUE;
                    prevMinY = minY = Double.MAX_VALUE;
                    prevMaxX = maxX = 0;
                    prevMaxY = maxY = 0;
                    prevCurrArea = currArea = 0.0;

                    selectedCSV = new ArrayList();
                }
            } while (!areaExided);

            writeCSVandMetadata(d, seed, selectedCSV, currArea, maxArea, minX, minY, maxX, maxY);
        }

    }

    /**
     *
     * @param d the desired dimension of the area
     * @param seed the seed used
     * @param selectedCSV selected lines, i.e. cells, from the original CSV file
     * @param selectedArea the area surface based on the selected cells from the
     * CSV. This is an approximation of targetArea
     * @param targetArea the targeted surface of the area, which may be a bit
     * different than the selectedArea
     * @param minX minimum X coordinate based on selected cells
     * @param minY minimum Y coordinate based on selected cells
     * @param maxX maximum X coordinate based on selected cells
     * @param maxY maximum Y coordinate based on selected cells
     */
    private static void writeCSVandMetadata(
            double d, int seed, List<double[]> selectedCSV,
            double selectedArea, double targetArea, double minX, double minY, double maxX, double maxY) {
        // if here, then CSV lines are selected and metadata too (min, max, etc..)
        String csvWrFile = "D:\\xvas\\dev\\MECOMM\\bsCSV\\"
                + "d=" + (int) d
                + "\\"
                + "seed=" + seed + ".log";

        File outputCSV = new File(csvWrFile);
        outputCSV.getParentFile().mkdirs();

        String name = outputCSV.getName();
        File metaCSV = new File(outputCSV.getParent(), name.substring(0, name.length() - 4) + ".meta");
        metaCSV.getParentFile().mkdir();

        System.out.println("Writting to: \"" + csvWrFile + "\"");
        System.out.println("#selected lines from original CSV trace: " + selectedCSV.size());

        try (
                FileWriter wr = new FileWriter(csvWrFile);
                FileWriter wrMeta = new FileWriter(metaCSV);) {

            int i = selectedCSV.size();
            for (double[] nxt : selectedCSV) {

                String line
                        = Arrays.toString(nxt)
                        .replaceAll("\\[", "")
                        .replaceAll("\\]", "")
                        .replaceAll(",", "");

                //  CSVUtils.writeLine(wr, Arrays.asList(line.substring(1, line.length() - 1)), ' ');
                wr.append(line.substring(1, line.length() - 1));
                if (--i > 0) {
                    wr.append('\n');
                }

            }

            char sep = '=';
            DecimalFormat df = new DecimalFormat("#");
            df.setMaximumFractionDigits(8);

            wrMeta.append("# cells");
            wrMeta.append(sep);
            wrMeta.append(String.valueOf(selectedCSV.size()));
            wrMeta.append('\n');

            wrMeta.append("% of total area");
            wrMeta.append(sep);
            double perc = ((int) selectedArea / targetArea * 10000) / 100.0;
            wrMeta.append(String.valueOf(perc));
            wrMeta.append('\n');

            wrMeta.append("square dimension");
            wrMeta.append(sep);
            wrMeta.append(String.valueOf(d));
            wrMeta.append('\n');

            wrMeta.append("targeted area");
            wrMeta.append(sep);
            wrMeta.append(
                    String.valueOf(df.format(
                            targetArea)
                    )
            );
            wrMeta.append('\n');

            wrMeta.append("approximated area");
            wrMeta.append(sep);
            wrMeta.append(
                    String.valueOf(df.format(
                            selectedArea)
                    )
            );
            wrMeta.append('\n');

            wrMeta.append("min coordinate X");
            wrMeta.append(sep);
            wrMeta.append(String.valueOf(minX));
            wrMeta.append('\n');

            wrMeta.append("min coordinate Y");
            wrMeta.append(sep);
            wrMeta.append(String.valueOf(minY));
            wrMeta.append('\n');

            wrMeta.append("max coordinate X");
            wrMeta.append(sep);
            wrMeta.append(String.valueOf(maxX));
            wrMeta.append('\n');

            wrMeta.append("max coordinate Y");
            wrMeta.append(sep);
            wrMeta.append(String.valueOf(maxY));
            wrMeta.append('\n');

            wrMeta.append("approximated area from");
            wrMeta.append(sep);
            wrMeta.append(String.valueOf(minX));
            wrMeta.append(',');
            wrMeta.append(String.valueOf(minY));
            wrMeta.append('\n');

            wrMeta.append("approximated area to");
            wrMeta.append(sep);
            wrMeta.append(String.valueOf(maxX));
            wrMeta.append(',');
            wrMeta.append(String.valueOf(maxY));
            wrMeta.append('\n');

//                    System.err.println(" maxArea " + maxArea);
//                    System.err.println(" maxX " + maxX);
//                    System.err.println(" maxY " + maxY);
//                    System.err.println(" minX " + minX);
//                    System.err.println(" minY " + minY);
            wr.flush();
            wr.close();
            wrMeta.flush();
            wrMeta.close();

        } catch (IOException ex) {
            Logger.getLogger(CellsCSVProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void parseCSV(String csvFile, List<double[]> lines) {
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line = "";
            String splitted = ",";

            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] lineArr = line.split(splitted);
                double[] lineNums = new double[3];
                int i = 0;
                for (String nxt : lineArr) {
                    double nxtDble = Double.parseDouble(nxt);
                    lineNums[i++] = nxtDble;
                }
                lines.add(lineNums);
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "", e);
        }
    }
    private static final Logger LOG = Logger.getLogger(CellsCSVProcessor.class.getName());

}
