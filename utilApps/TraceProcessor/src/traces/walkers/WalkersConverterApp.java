package traces.walkers;

import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import utilities.Couple;

/**
 *
 * @author xvas
 */
public class WalkersConverterApp {

    public static int TU = 60;// time units, i.e. how many seconds map to one simulation time unit
    public static int TIME_MULT = 1;// converts double times in trace, e.g. 0.6, to integer times for the simulator, e.g. 6. dx/dt and dy/dt must be also multipled to reverse the effect of mu;tiplying the time

    public static String epikefalida = "#TIME,UID,dx/dt,dy/dt\n#TU=" + TU;
    public static String base = (System.getProperty("os.name").toLowerCase().startsWith("linux")
            ? "/home/xvas/Dropbox"
            : "C:/Users/xvas/Dropbox") + "/2014-2015-EPC+POP/trunk/files/unsync/walkers/";

    public static void main(String[] args) throws FileNotFoundException, IOException {

//        String choose = "/subway";
       String choose =  "/dense";
//                String choose =  "/medium";
//        String choose =  "/sparse";
        String parentPath = base + choose;

        Map<Long, String> woutGlobalSet = new TreeMap();
        woutGlobalSet.put(-1L, epikefalida + "\n");

        double lastFileMaxTime = 0;

        File parent = new File(parentPath);
        File processDir = new File(parent.getParentFile().getCanonicalPath() + "/" + parent.getName() + "/" + choose + "/");
        processDir.mkdirs();
        FileWriter woutGlobal = new FileWriter(processDir.getCanonicalPath() + "/merged.csv");

        for (File nxtInputFile : parent.listFiles()) {
            if (nxtInputFile.isDirectory()) {
                continue;
            }
            String outFilePath = processDir.getCanonicalPath() + "/" + nxtInputFile.getName() + ".csv";
//            lastFileMaxTime = 
                    processFile(nxtInputFile, outFilePath, lastFileMaxTime, woutGlobalSet);
        }

        for (Long nxt : woutGlobalSet.keySet()) {
            woutGlobal.append(woutGlobalSet.get(nxt));
        }

        woutGlobal.close();

        Toolkit.getDefaultToolkit().beep();
    }

    public static double processFile(File inputFile, String outFilePath,
            double initGlobalTime, Map<Long, String> woutGlobalMap)
            throws FileNotFoundException, IOException, NumberFormatException {

        int maxId = -1;

        Map<Integer, Couple<Double, Double>> prevCoords = new HashMap();
        Map<Integer, Double> prevTime4Coords = new HashMap();

        BufferedReader bin = new BufferedReader(new FileReader(inputFile));
        System.out.println("");
        System.err.println("");
        Logger.getLogger(WalkersConverterApp.class.getName()).log(Level.INFO, "Start processing file \"{0}\" using initTime={1}", new Object[]{inputFile.getCanonicalPath(), initGlobalTime});
        int countBinLines = 0;

        File fout = new File(outFilePath);

        FileWriter woutLcl = new FileWriter(fout);
        Map<Long, String> woutLclMap = new TreeMap<>();
        woutLclMap.put(-1L, epikefalida + '\n');

        String line = null;
        double maxTime = 0;
        while ((line = bin.readLine()) != null) {
            String[] split = line.split(" ");

            double time = Double.parseDouble(split[0]) * TIME_MULT;
            maxTime = Math.max(maxTime, time);

            String event = split[1];

            int id = -1;

            switch (event) {

                case "create":
                    id = Integer.parseInt(split[2]);
                    double x = Double.parseDouble(split[3]);
                    double y = Double.parseDouble(split[4]);

                    prevCoords.put(id, new Couple(x, y));
                    prevTime4Coords.put(id, time);

                    break;

                case "setdest":
                    id = Integer.parseInt(split[2]);
                    x = Double.parseDouble(split[3]);
                    y = Double.parseDouble(split[4]);

                    Couple<Double, Double> prevXY = prevCoords.get(id);
                    double prevTime = prevTime4Coords.get(id);

                    double dt = 0;
                    if (prevTime == time) {
                        continue;
                    } else if (prevTime < time) {
                        dt = time - prevTime; // +1 to avoid zeros
                    } else {
                        throw new IOException("prev time >  time: " + prevTime + ">" + time);
                    }

                    double dx = x - prevXY.getFirst();
                    double dy = y - prevXY.getSecond();

                    double dxdt = Math.round(100.0 * TU * TIME_MULT * dx / dt) / 100.0;
                    double dydt = Math.round(100.0 * TU * TIME_MULT * dy / dt) / 100.0;

                    StringBuilder sb = new StringBuilder();
                    StringBuilder sbGlob = new StringBuilder();

                    sb.append((long) time).append(",");
                    sbGlob.append((long) (time + initGlobalTime)).append(",");

                    sb.append(id).append(",");
                    sbGlob.append(id).append(",");

                    sb.append(dxdt).append(",");
                    sbGlob.append(dxdt).append(",");

                    sb.append(dydt).append("\n");
                    sbGlob.append(dydt).append("\n");

                    woutLclMap.put((long) time, sbGlob.toString());
                    woutGlobalMap.put((long) (time + initGlobalTime), sbGlob.toString());

                    prevCoords.put(id, new Couple(x, y));
                    prevTime4Coords.put(id, time);
                    break;

                case "destroy":
                    continue;

                default:
                    throw new IOException("Unknown type of event: " + event);

            }

            maxId = Math.max(maxId, id);

            if (++countBinLines % 25000 == 0) {
                System.out.print(".");
            }
        }

        for (Long nxt : woutLclMap.keySet()) {
            woutLcl.append(woutLclMap.get(nxt));
        }

        woutLcl.append("#Num," + maxId);
        woutLcl.close();
        bin.close();
        System.out.println("");
        System.err.println("");
        Logger.getLogger(WalkersConverterApp.class.getName()).log(Level.INFO, "Finished processing file. Output added to path: \"{0}\"", outFilePath);

        return maxTime + initGlobalTime;

    }
}
