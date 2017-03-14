package traces.taxi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
public class TaxiFileProcessor {

    private static final TaxiFileProcessor singleton = new TaxiFileProcessor();

    private static final Logger LOGGER = Logger.getLogger(TaxiFileProcessor.class.getCanonicalName());

    private int _coresAvailable = Runtime.getRuntime().availableProcessors() - 1;
    private int _running = 0;

    protected static class TupleRead {

        double _x;
        double _y;
        int _occupied;
        long _time;

        public TupleRead(double x, double y, int occupied, long tm) {
            this._x = x;
            this._y = y;
            this._occupied = occupied;
            this._time = tm;
        }

    }

    protected static class TupleProccessedAggr {

        private final long _time;
        private final int _uID;
        private final double _dt;
        private final double _dxdt;
        private final double _dydt;

        private TupleProccessedAggr(long time, double dt, int uID, double dx, double dy) {
            _time = time;
            _uID = uID;
            _dt = dt;
            _dxdt = dx;
            _dydt = dy;
        }

        @Override
        public String toString() {
            return "_time=" + _time + ","
                    + "_uID=" + _uID + ","
                    + "_dt=" + _dt + ","
                    + "_dxdt=" + _dxdt + ","
                    + "_dydt=" + _dydt;
        }

        /**
         * @return the _time
         */
        public long getTime() {
            return _time;
        }

        /**
         * @return the _uID
         */
        public int getuID() {
            return _uID;
        }

        /**
         * @return the _dxdt
         */
        public double getDxdt() {
            return _dxdt;
        }

        /**
         * @return the _dydt
         */
        public double getDydt() {
            return _dydt;
        }

        /**
         * @return the _dt
         */
        public double getDt() {
            return _dt;
        }
    }

    private TaxiFileProcessor() {
    }

    public static void appendMetadata(final FileWriter meta, String[] processed) {
        try {
            ////////////print metadata for each processed file
            StringBuilder msg = new StringBuilder();

            int i = 0;
            for (i = 0; i < processed.length - 1; i++) {
                meta.append(processed[i] + ",");
                msg.append("{").append(i).append("}, ");
            }
            meta.append(processed[i] + "\n");
            msg.append("{").append(i).append("}\n");

            String msgStr = "Metadata appened: " + msg.toString();
            LOGGER.log(Level.INFO, msgStr, processed);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    public static TaxiFileProcessor instance() {
        return singleton;
    }

    protected String metaTitles() {
        return "UID,filename,consideredRecs,unconsideredRecs,avgDxDt,stdevDxDt,avgDyDt,stdevDyDt,dtMax,DxDtMax,DyDtMax,MinX,MinY,MaxX,MaxY,AreaX,AreaY,AreaDiagonal";
    }

    /**
     * @return the _running
     */
    public synchronized int getRunning() {
        return _running;
    }

    private SortedMap<Long, TupleRead> parseAndSortByTime(final BufferedReader in, File inputFile) throws NumberFormatException, IOException {
        String nxtLine = in.readLine();

        SortedMap<Long, TupleRead> sortedByTime = Collections.synchronizedSortedMap(new TreeMap<>());
        // sort by time first
        do {
            String[] csvLine = nxtLine.split(" ");

            try {
                double nxtX = Double.parseDouble(csvLine[0]);
                double nxtY = Double.parseDouble(csvLine[1]);

                int occupied = Integer.parseInt(csvLine[2]);

                long nxtTime = Long.parseLong(csvLine[3]);

                sortedByTime.put(nxtTime, new TupleRead(nxtX, nxtY, occupied, nxtTime));

            } catch (NumberFormatException nfe) {
                throw new IOException("Can not parse file " + inputFile.getCanonicalPath(), nfe);
            }
        } while ((nxtLine = in.readLine()) != null);

        in.close();

        return sortedByTime;
    }

    private double distInMeters(double y1, double x1, double y2, double x2) {  // generally used geo measurement function

        double earthR = 6378.137; // Earth's radius in KM

        double dLat = (y2 - y1) * Math.PI / 180; // diff se aktinia
        double dLon = (x2 - x1) * Math.PI / 180; // diff se aktinia

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(y1 * Math.PI / 180) * Math.cos(y2 * Math.PI / 180)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double d = earthR * c;

        return d * 1000; // se metra
    }

    private double[] computeAggrStats(double dxMetersSum, int counter, double dyMetersSum, List<Double> dxdtList, List<Double> dydtList) {
        double avgDxDt;
        double avgDyDt;
        double stdevDxDt;
        double stdevDyDt;
        avgDxDt = dxMetersSum / counter;
        avgDyDt = dyMetersSum / counter;
        stdevDxDt = 0.0;

        for (Double val : dxdtList) {
            stdevDxDt += Math.pow(avgDxDt - val, 2);
        }
        stdevDxDt = Math.sqrt(stdevDxDt / counter);
        stdevDyDt = 0.0;

        for (Double val : dydtList) {
            stdevDyDt += Math.pow(avgDyDt - val, 2);
        }
        stdevDyDt = Math.sqrt(stdevDyDt / counter);

        return new double[]{
            avgDxDt,
            stdevDxDt,
            avgDyDt, stdevDyDt};
    }

    public static Runnable processInParallel(
            final FileWriter meta, File inputFile, File outputFile,
            SortedMap<Long, List<TupleProccessedAggr>> sortedByTimeAggregates,
            int uID, String workerName)
            throws FileNotFoundException, IOException {

        TaxiFileProcessor lock = instance();

        synchronized (lock) {
            lock._coresAvailable--;

            LOGGER.log(Level.INFO, "Worker thread: " + workerName + " Currently unallocated processor cores: {0}", lock._coresAvailable);

            while (lock._coresAvailable <= 0 && !Thread.currentThread().isInterrupted()) {
                try {
                    lock.wait();
                } catch (InterruptedException ex) {
                    // ignore
                }
            }
        }

        return (new Runnable() {
            public void run() {
                synchronized (lock) {
                    lock._running++;

                    String[] processResults = lock.process(inputFile, outputFile, sortedByTimeAggregates, uID);
                    appendMetadata(meta, processResults);

                    lock._coresAvailable++;
                    lock.notifyAll();
                    lock._running--;
                }
            }
        });
    }

    public String[] process(File inputFile, File outputFile, SortedMap<Long, List<TupleProccessedAggr>> sortedByTimeAggregates, int uID) {

        double dtMax;
        double dxdtMax;
        double dydtMax;
        double xMax;
        double yMax;
        double xMin;
        double yMin;

        try (FileWriter out = new FileWriter(outputFile); BufferedReader in = new BufferedReader(new FileReader(inputFile))) {

            LOGGER.info("Processing file "
                    + "\"" + inputFile.getCanonicalPath() + "\""
                    + " to \""
                    + "\"" + outputFile.getAbsolutePath() + "\""
            );

            SortedMap<Long, TupleRead> sortedByTime = parseAndSortByTime(in, inputFile);

//////////// used for computing stddev and average            
            double dxdtSum = 0;
            dtMax = 0;
            dxdtMax = 0;
            List<Double> dxdtList = new ArrayList();
            double dydtSum = 0;
            dydtMax = 0;
            List<Double> dydtList = new ArrayList();

            out.write("#TIME,");
//            out.write("dt,");
            out.write("UID,");
            out.write("dx/dt,");
            out.write("dy/dt,");
            out.write("xMin,");
            out.write("xMax,");
            out.write("yMin,");
            out.write("yMax\n");

            // read first sorted values
            TupleRead first = sortedByTime.remove(sortedByTime.firstKey());
            double prevX = first._x;
            xMin = xMax = first._x;

            double prevY = first._y;
            yMin = yMax = first._y;

            long prevTime = first._time;

            long baseTime = first._time;

            int consideredRecs = 1;

            int unconsideredRecs = 0;

            for (Map.Entry<Long, TupleRead> entry : sortedByTime.entrySet()) {

                ///////////////////// KOFTHS //////////////////////
                //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx//
                if (consideredRecs > 3000) {
                    break;
                }
                //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx//

                TupleRead nxt = entry.getValue();

                double nxtX = nxt._x;
                double nxtY = nxt._y;
                long nxtTime = nxt._time;

                int dt = (int) (nxtTime - prevTime);
                double dx = distInMeters(0, prevX, 0, nxtX);
                double dy = distInMeters(prevY, 0, nxtY, 0);

               // boolean timeViolatation = dt > 120;
                boolean speedViolatation = Math.sqrt(dy * dy + dx * dx) / (double)dt > 55.56; // 27.78m/s => ~100km/h => 1668m max distance in one simulation hop. 41.67 => 150KM/h

                if (
//                        timeViolatation && 
                        speedViolatation) {
                    unconsideredRecs++;

                    prevTime = nxtTime;
                    prevX = nxtX;
                    prevY = nxtY;

                    continue;
                }
                consideredRecs++;

                double dxdt = ProcessorApp.TU * dx / dt;
                double dydt = ProcessorApp.TU * dy / dt;

                dxdtSum += dxdt;
                dxdtList.add(dxdt);

                dydtSum += dydt;
                dydtList.add(dydt);

                if (prevX < xMin) {
                    xMin = prevX;
                }

                if (prevX > xMax) {
                    xMax = prevX;
                }

                if (prevY < yMin) {
                    yMin = prevY;
                }

                if (prevY > yMax) {
                    yMax = prevY;
                }

                if (dt > dtMax) {
                    dtMax = dt;
                }
                if (dxdt > dxdtMax) {
                    dxdtMax = dxdt;
                }
                if (dydt > dydtMax) {
                    dydtMax = dydt;
                }

                long time = prevTime - baseTime;

////////////////print the change in coordinates after this time
                out.write(time + ",");
                out.write(uID + ",");
                out.write(dxdt + ",");
                out.write(dydt + "\n");

                List<TupleProccessedAggr> otherTuples;
                if ((otherTuples = sortedByTimeAggregates.get(time)) == null) {
                    sortedByTimeAggregates.put(time, otherTuples = new ArrayList<>());
                }

                TupleProccessedAggr tuple = new TupleProccessedAggr(time, dt, uID, dxdt, dydt);

                otherTuples.add(tuple);

                prevTime = nxtTime;
                prevX = nxtX;
                prevY = nxtY;
            }

            double[] aggrStats = computeAggrStats(dxdtSum, consideredRecs, dydtSum, dxdtList, dydtList);
            String[] result = new String[18];

            int i = 0;
            result[i++] = String.valueOf(uID);
            result[i++] = String.valueOf(inputFile.getName());
            result[i++] = String.valueOf(consideredRecs);
            result[i++] = String.valueOf(unconsideredRecs);
            result[i++] = String.valueOf(aggrStats[0]);
            result[i++] = String.valueOf(aggrStats[1]);
            result[i++] = String.valueOf(aggrStats[2]);
            result[i++] = String.valueOf(aggrStats[3]);
            result[i++] = String.valueOf(dtMax);
            result[i++] = String.valueOf(dxdtMax);
            result[i++] = String.valueOf(dydtMax);
            result[i++] = String.valueOf(xMin);
            result[i++] = String.valueOf(yMin);
            result[i++] = String.valueOf(xMax);
            result[i++] = String.valueOf(yMax);
            result[i++] = String.valueOf(distInMeters(yMin, xMin, yMax, xMax));
            result[i++] = String.valueOf(distInMeters(0, xMin, 0, xMax));
            result[i++] = String.valueOf(distInMeters(yMin, 0, yMax, 0));

            out.append("#" + metaTitles() + "\n" + "#");
            for (int j = 0; j < result.length; j++) {
                out.append(result[j] + ",");
            }

            LOGGER.log(Level.INFO, "{0} processed. Output found at: {1}",
                    new Object[]{inputFile.getName(),
                        outputFile.getCanonicalPath()}
            );

            return result;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Exception in Processing file ", ex);
            return null;
        }//try with resources: in; out

    }

}
