/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package traces.taxi;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author xvas
 */
public class ProcessorApp {
    public static double TU = 10.0;// seconds per simulation time unit. I.E. defines dt in dx/dt and dy/dt.

    private static final Logger LOGGER = Logger.getLogger(ProcessorApp.class.getCanonicalName());

    static {
        LOGGER.setLevel(Level.FINE);
    }

    public static String NUM = "Num";

    private static void printAllCSV(File allFile, final FileWriter all,
            SortedMap<Long, List<TaxiFileProcessor.TupleProccessedAggr>> sortedByTimeAggregates) throws Exception {
        LOGGER.log(Level.INFO, "Printing all records to {0}", allFile.getCanonicalPath());

        all.append("#TIME,UID,dx/dt,dy/dt" + "\n");
        int count = 0;

//        synchronized (sortedByTimeAggregates) {
        for (Map.Entry<Long, List<TaxiFileProcessor.TupleProccessedAggr>> entry : sortedByTimeAggregates.entrySet()) {
            List<TaxiFileProcessor.TupleProccessedAggr> nxtTupleList = entry.getValue();

            for (TaxiFileProcessor.TupleProccessedAggr nxtTuple : nxtTupleList) {
                all.append(nxtTuple.getTime() + ",");
                all.append(nxtTuple.getuID() + ",");

                double dt = nxtTuple.getDt();
                double dxdt = nxtTuple.getDxdt() / dt;
                double dydt = nxtTuple.getDydt() / dt;

                all.append(dxdt + ",");
                all.append(dydt + "\n");
            }
            if (LOGGER.isLoggable(Level.FINE) && ++count % sortedByTimeAggregates.size() == 1000) {
                double percent = Math.round(10.0 * count / sortedByTimeAggregates.size()) / 10.0;
                LOGGER.log(Level.FINE, "{0}% printed in {1}. Another {2} records left to print.",
                        new Object[]{percent, allFile.getName(), sortedByTimeAggregates.size() - count});
            }
        }
//        }
        LOGGER.log(Level.INFO, "All records printed to {0}", allFile.getCanonicalPath());
    }

    public static void main(String[] args) throws IOException {
        TaxiFileProcessor taxiFileProc = TaxiFileProcessor.instance();
        List<Thread> workers = new ArrayList<>();

        SortedMap<Long, List<TaxiFileProcessor.TupleProccessedAggr>> sortedByTimeAggregates = Collections.synchronizedSortedMap(new TreeMap<>());

        String basePath
                = (System.getProperty("os.name").equals("Linux") ? "/home/" : "C:/Users/")
                + "xvas/Dropbox/2014-2015-EPC+POP/"
                + "trunk/"
                + "files/unsync/"
                + "taxiTrace";

        File allFile = new File(basePath + "/taxis.csv");

        try (
                FileWriter meta = new FileWriter(basePath + "/taxis_meta.csv");
                FileWriter all = new FileWriter(allFile)) {

            File dir = new File(basePath + "/cabspottingdata/");

            meta.write("#" + taxiFileProc.metaTitles() + "\n");

////////////process each file          
            int uID = 0;// the user id to use for the next taxi file
            File[] gpsFilesList = dir.listFiles();
            meta.write("#" + NUM + "," + gpsFilesList.length + "\n");

            for (File finGps : gpsFilesList) {
                if (finGps.getName().startsWith("_")) {
                    continue;
                }

                File processedDir = new File(
                        basePath
                        + "/processed");
                if (!processedDir.exists()) {
                    processedDir.mkdirs();
                }
                File foutProcessed = new File(
                        processedDir,
                        finGps.getName().substring(0, finGps.getName().length() - 4) + ".csv");

////////////convert and output each file            
                String workerName = "Thread-" + finGps.getName();
                Runnable nxtProccess = TaxiFileProcessor.processInParallel(meta, finGps, foutProcessed, sortedByTimeAggregates, ++uID, workerName);
                Thread t;
                workers.add(t = new Thread(nxtProccess, workerName));
                t.start();
            }//for
////////////all tasks have finished or the time has been reached.

            for (Thread worker : workers) {
                worker.join();
            }

            ////////////print all.csv
            printAllCSV(allFile, all, sortedByTimeAggregates);

        }//try
        catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            LOGGER.log(Level.SEVERE, sw.toString(), e);
        }
    }//main

}
