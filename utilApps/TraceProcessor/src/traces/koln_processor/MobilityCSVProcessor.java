/**
 * 2017-03-04
 *
 * @author xvas
 */
package traces.koln_processor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MobilityCSVProcessor {

    public static FileWriter fwr;
    public static BufferedWriter bwr = null;

    /**
     * ID, x, y, speed
     *
     * @param args
     */
    public static void main(String[] args) {
        String home = System.getProperty("user.home");
        String base = home + "/Dropbox/EPC/git/EPM";
        String originalCSVFile = base + "/MECOMM/koln_tr/koln.tr";
        String metaDirPath = base + "/MECOMM/bsCSV/d=1000/";

        File metaDir = new File(metaDirPath);
        for (String fileName : metaDir.list()) {

            if (!fileName.endsWith("meta")) {
                //use only meta data files
                continue;
            }

            String metaFilePath = metaDirPath + fileName;

            Map<String, Double> metadata = new HashMap<>();

            parseMetadata(metaFilePath, metadata);

            double minX = metadata.get("min coordinate X");
            double minY = metadata.get("min coordinate Y");
            double maxX = metadata.get("max coordinate X");
            double maxY = metadata.get("max coordinate Y");

            processMobilityData(originalCSVFile, minX, minY, maxX, maxY, fileName, metaDirPath);
        }
    }

    private static String preparePathStreamToOutputMobTrace(String metaDirPath, String fileName) {
        String mobResultFilePath = metaDirPath + fileName.substring(0, fileName.length() - 5).concat(".tr");
        try {
            fwr = new FileWriter(mobResultFilePath);
            bwr = new BufferedWriter(fwr);
        } catch (IOException ex) {
            Logger.getLogger(MobilityCSVProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
        return mobResultFilePath;
    }

    private static void closePathStreamToOutputMobTrace() {
        try {
            bwr.close();
        } catch (IOException ex) {
            Logger.getLogger(MobilityCSVProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void processMobilityData(String csvRdFile, double minX, double minY, double maxX, double maxY, String fileName, String metaDirPath) {

        String mobResultFilePath = preparePathStreamToOutputMobTrace(metaDirPath, fileName);

        List<MobCSVLine> selectedMobCSVLines = new ArrayList();

        MobilityCSVProcessor csvLoader = new MobilityCSVProcessor();

        csvLoader.parseCSVMobility(csvRdFile, mobResultFilePath, selectedMobCSVLines, minX, minY, maxX, maxY);

        closePathStreamToOutputMobTrace();
    }

    private static void writeCSVandMetadata(String csvWrFile, List<MobCSVLine> selectedCSVLines, int i) {

        File outputCSV = new File(csvWrFile);
        outputCSV.getParentFile().mkdirs();

        String name = outputCSV.getName();
        File metaCSV = new File(outputCSV.getParent(), name.substring(0, name.length() - 4) + ".meta");
        metaCSV.getParentFile().mkdir();

        if (i % 1000000 == 0) {
            LOG.log(Level.INFO, "\n\nAppending lines to: \"{0}\""
                    + "\n#added {1} more lines from original CSV.. "
                    + "\nFirst line from trace to add: {2}"
                    + "\nLast line from trace to add: {3}",
                    new Object[]{
                        csvWrFile,
                        selectedCSVLines.size(),
                        selectedCSVLines.get(0).toString(),
                        selectedCSVLines.get(selectedCSVLines.size() - 1).toString()}
            );
        }
        try {

            for (MobCSVLine nxt : selectedCSVLines) {

                //use blank space as separator
                StringBuilder line = new StringBuilder();
                line.append(nxt.time);
                line.append(' ');

                line.append(nxt.id);
                line.append(' ');

                line.append(nxt.x);
                line.append(' ');

                line.append(nxt.y);
                line.append(' ');

                line.append(nxt.speed);
                line.append(' ');

                //  CSVUtils.writeLine(wr, Arrays.asList(line.substring(1, line.length() - 1)), ' ');
                bwr.append(line.toString()).append('\n');

            }

            bwr.flush();

        } catch (IOException ex) {
            Logger.getLogger(MobilityCSVProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static final Logger LOG = Logger.getLogger(MobilityCSVProcessor.class.getName());

    private static void parseMetadata(String csvFile, Map<String, Double> metadata) {

        Logger.getLogger(MobilityCSVProcessor.class.getName()).log(
                Level.INFO, "Parsing metadata from file: \"{0}\"\n", csvFile);

        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = "=";

        try {

            br = new BufferedReader(new FileReader(csvFile));
            int i = 0;
            while ((line = br.readLine()) != null) {
                i++;

                // use comma as separator
                String[] lineArr = line.split(cvsSplitBy);
                if (lineArr.length != 2) {
                    Logger.getLogger(MobilityCSVProcessor.class.getName())
                            .log(Level.SEVERE,
                                    "Line {0} in metadata file \"{1}\" ignored: \"{2}\"",
                                    new Object[]{i, csvFile, lineArr[0]}
                            );
                }

                //hack TODO take this off the metadata lines. It is needless..
                if (lineArr[0].startsWith("approximated area")) {
                    continue;
                }

                metadata.put(lineArr[0], Double.parseDouble(lineArr[1]));
            }

        } catch (FileNotFoundException e) {
            //e.printStackTrace();
            Logger.getLogger(MobilityCSVProcessor.class.getName())
                    .log(Level.SEVERE,
                            "File not found: \"" + csvFile + "\"", e
                    );
        } catch (IOException e) {
            Logger.getLogger(MobilityCSVProcessor.class.getName())
                    .log(Level.SEVERE,
                            null, e
                    );
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    Logger.getLogger(MobilityCSVProcessor.class.getName())
                            .log(Level.SEVERE,
                                    null, e
                            );
                }
            }
        }
    }

    /**
     * CSV inn the form: time, ID, x, y, seed
     *
     * @param csvRdFile
     * @param mobCSVSelectedLines
     * @param minX
     * @param minY
     * @param maxX
     * @param maxY
     */
    private void parseCSVMobility(String inputCSVFile, String mobResultFilePath,
            List<MobCSVLine> selectedMobCSVLines,
            double minX, double minY, double maxX, double maxY) {

        BufferedReader br = null;
        String nxtLine = "";
        String cvsSplitBy = " ";

        try {

            br = new BufferedReader(new FileReader(inputCSVFile));
            int i = 0;
            while ((nxtLine = br.readLine()) != null) {
                i++;

                if (i % 1000000 == 0) {
                    Logger.getLogger(MobilityCSVProcessor.class.getName())
                            .log(Level.INFO,
                                    "Parsed input mobility trace line #{0}", i
                            );

                    writeCSVandMetadata(mobResultFilePath, selectedMobCSVLines, i);

                    selectedMobCSVLines.clear();
                }

                String[] lineArr = nxtLine.split(cvsSplitBy);
                if (lineArr.length != 5) {
                    Logger.getLogger(MobilityCSVProcessor.class.getName())
                            .log(Level.WARNING,
                                    "Wrong number of ellements in CSV line of mobility file. Ignored line {0}: \"{1}\"", new Object[]{i, nxtLine}
                            );
                    continue;
                }

                if (!withinBounds(lineArr, minX, minY, maxX, maxY)) {
                    continue;
                }

                selectedMobCSVLines.add(new MobCSVLine(
                        Integer.parseInt(lineArr[0]),
                        lineArr[1],
                        Double.parseDouble(lineArr[2]),
                        Double.parseDouble(lineArr[3]),
                        Double.parseDouble(lineArr[4])
                ));

            }

        } catch (FileNotFoundException e) {
            //e.printStackTrace();
            Logger.getLogger(MobilityCSVProcessor.class.getName())
                    .log(Level.SEVERE,
                            "File not found: \"" + inputCSVFile + "\"", e
                    );
        } catch (IOException e) {
            Logger.getLogger(MobilityCSVProcessor.class.getName())
                    .log(Level.SEVERE,
                            null, e
                    );
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    Logger.getLogger(MobilityCSVProcessor.class.getName())
                            .log(Level.SEVERE,
                                    null, e
                            );
                }
            }
        }
    }

    private boolean withinBounds(String[] lineArr, double minX, double minY, double maxX, double maxY) {
        double x = Double.parseDouble(lineArr[2]);
        double y = Double.parseDouble(lineArr[3]);

        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }

    private static class MobCSVLine {

        private int time;
        private String id;
        private double x;
        private double y;
        private double speed;

        public MobCSVLine(int time, String id, double x, double y, double speed) {
            this.time = time;
            this.id = id;
            this.x = x;
            this.y = y;
            this.speed = speed;
        }

        /**
         * @return the time
         */
        public int getTime() {
            return time;
        }

        /**
         * @return the id
         */
        public String getId() {
            return id;
        }

        /**
         * @return the x
         */
        public double getX() {
            return x;
        }

        /**
         * @return the y
         */
        public double getY() {
            return y;
        }

        /**
         * @return the speed
         */
        public double getSpeed() {
            return speed;
        }

        @Override
        public String toString() {
            return "MobCSVLine{" + "time=" + time + ", id=" + id + ", x=" + x + ", y=" + y + ", speed=" + speed + '}';
        }

    }

}
