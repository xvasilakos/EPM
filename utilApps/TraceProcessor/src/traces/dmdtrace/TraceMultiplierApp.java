package traces.dmdtrace;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * To multiply a trace and corresponding workload
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class TraceMultiplierApp {

    public static double ZIPF_PARAM_S = 0.90;
    /**
     * This is the desired size of the reproduced files in the video traces in MB. 
     * If this is set to 30, then we need to use a thirtyMBMultiplier = 2.96. For
     * different desired file sizes, e.g. 240, one trick is to use
     * desiredFileSize / 30.0 * thirtyMBMultiplier
     */
    public static final int DESIRED_FILE_SIZE = 500;

    /**
     * Based on the original trace, multiplying the _size of each file by x 2.96
     * yields a mean file _size of 30MB.
     */
    public static final double THIRTY_MB_MULTIPLIER = 2.96;
    public static final String NEW_DIR = DESIRED_FILE_SIZE + "MB"+"_zipfS_" + ZIPF_PARAM_S ;

    /**
     * The number of times that the _size of each file will be increased. This
     * refers to the median file _size.
     */
    public static final double VIDEO_FILE_SIZE_MULTIPLIER = ((double) DESIRED_FILE_SIZE ) * THIRTY_MB_MULTIPLIER / 30.0;

    /**
     * idGen Choose a sufficiently big number from which the IDs of the added
     * doc records will be started. For instance, the max id in the traces will
     * do.
     */
    public static final long INITIAL_ID_GENERATOR = 271813;// This is the max id in the traces
    /**
     * This is the ID generator basis. To see how new IDs are generated, check
     * the apropriate static method use for the reason.
     *
     * @see TraceMultiplierApp#generateNewID(int)
     */
    private static long idGen = INITIAL_ID_GENERATOR;
//    private static final  String VIDEO_TRACE_BASE_PATH = "C:/Users/xvas/Dropbox/2014-2015-EPC+POP/trunk/files/dmdtrace/video";
    private static final String ROOT = System.getProperty("os.name").equalsIgnoreCase("LINUX") ? "/home" : "C:/Users";
    private static final String VIDEO_TRACE_BASE_PATH = ROOT + "/xvas/Dropbox/2014-2015-EPC+POP/trunk/files/dmdtrace/video";
    private static long _lastLogged = 0;

    public static void main(String[] args) {
        String traceFileName;

        final int total = 29 * 5;
        int left = 29 * 5;

//        int start = 111;//111 gives x112=> x5 #videos in VC x24
//        for (int timesI = start; timesI > 110; timesI-=10) {
        int start = 23;//23->24
        for (int timesI = start; timesI > 22; timesI-=10) {
            for (int traceNameJ = 0; traceNameJ < 5; traceNameJ++) {
                traceFileName = String.valueOf(traceNameJ);
                run(timesI, traceFileName, VIDEO_FILE_SIZE_MULTIPLIER);
            }
            left--;
            if (_lastLogged - System.currentTimeMillis() > 5000) {
                _lastLogged = System.currentTimeMillis();
                Logger.getLogger(TraceMultiplierApp.class.toString()).log(Level.INFO,
                        "Done: \n" + (Math.round(10000.0 * left / total) / 100));
            }

        }
    }

    /**
     * @param times The number of time that each record is "cloned" into added
     * new tuples (plus the original record).
     * @param traceFileName
     * @param videoFileSizeMultiplier the number of times that the _size of each
     * file will be increased
     */
    public static void run(int times, String traceFileName, double videoFileSizeMultiplier) {

        Map<Long, Map<Long, Integer>> old2newIDsxTimes = new HashMap<>(); // old ID -> {new ID -> {how many times this new ID must appear}}
        //  ID
        //  Popularity (in #Requests)
        //  Size (in #Bytes)
        //  AppType
        File docsOriginal = new File(VIDEO_TRACE_BASE_PATH + "/docs/" + traceFileName + ".csv");
        //  File docs = new File("/home/xvas/Dropbox/2014-2015-EPC+POP/trunk/files/"
        //  + "dmdtrace/video/docsx/" + originalTraceName + "/30MB/x1.csv");

        File workloadOriginal = new File(VIDEO_TRACE_BASE_PATH + "/workload/" + traceFileName + ".csv");
        File docsX_out = new File(VIDEO_TRACE_BASE_PATH + "/docsxx/" + traceFileName + "/" + NEW_DIR + "/x" + (times + 1) + ".csv");
        File workloadX_out = new File(VIDEO_TRACE_BASE_PATH + "/workloadxx/" + traceFileName + "/" + NEW_DIR + "/x" + (times + 1) + ".csv");

        docsX_out.getParentFile().mkdirs();
        workloadX_out.getParentFile().mkdirs();

        
        System.err.println("docsX_out="+docsX_out.getAbsolutePath());
        System.err.println("workloadX_out="+workloadX_out.getAbsolutePath());
        
        
        try (
                BufferedReader docsIn = new BufferedReader(new FileReader(docsOriginal));
                BufferedReader workIn = new BufferedReader(new FileReader(workloadOriginal));
                BufferedWriter docout = new BufferedWriter(new FileWriter(docsX_out));
                BufferedWriter workout = new BufferedWriter(new FileWriter(workloadX_out))) {

            DocTuple specialTuple = loadDocs_CreateDocsxxCSV(docsIn, times, old2newIDsxTimes, docout, videoFileSizeMultiplier);
            loadWorkload_CreateWorkloadxxCSV(specialTuple._pop, workIn, old2newIDsxTimes, workout, videoFileSizeMultiplier);
        } catch (IOException e) {
            Logger.getLogger("TraceMultiplierApp").log(Level.SEVERE, e.toString());
            e.printStackTrace();
        }

    }

    /**
     *
     * @param docsIn
     * @param docSet
     * @param times The number of time that each record is "cloned" into added
     * new tuples (plus the original record).
     * @param old2newIDsxTimes
     * @param docout
     * @param videoFileSizeMultiplier the number of times that the _size of each
     * file will be increased
     * @throws IOException
     */
    private static DocTuple loadDocs_CreateDocsxxCSV(
            final BufferedReader docsIn, int times,
            Map<Long, Map<Long, Integer>> old2newIDsxTimes,
            final BufferedWriter docout, double videoFileSizeMultiplier
    ) throws IOException {

        SortedSet<DocTuple> mulitpliedSet = new TreeSet<>(new DocTuple.POP_COMPARATOR()); // use TreeMap to to keep order by number of requests in documents pool
        SortedSet<DocTuple> docSetOriginal = new TreeSet<>(new DocTuple.POP_COMPARATOR()); // use TreeMap to to keep order by number of requests in documents pool

        int popSum = 0;

        String line;
        while ((line = docsIn.readLine()) != null) {
            StringTokenizer toks = new StringTokenizer(line, ",\t");
            try {
                Long originalID = Long.parseLong(toks.nextToken());
                Long pop /*times requested*/ = Long.parseLong(toks.nextToken());
                Long size /*file _size*/ = (long) (Long.parseLong(toks.nextToken()) * videoFileSizeMultiplier);
                Long app /*application type; we expect this to be irrelevant as all records refer to videos*/ = Long.parseLong(toks.nextToken());

                DocTuple tuple = new DocTuple();
                tuple._id = originalID;
                tuple._pop = pop;
                tuple._size = size;
                tuple._app = app;

                popSum += pop;
                
                docSetOriginal.add(tuple);
            } catch (Exception e) {

                StringWriter wtr = new StringWriter();
                PrintWriter p = new PrintWriter(wtr);
                e.printStackTrace(p);
                Logger.getLogger(TraceMultiplierApp.class.toString()).log(Level.WARNING, "line ignored in trace of docs: " + line + "\n" + wtr.toString());
//                e.printStackTrace();
//               System.exit(-1);

            }
        }

        int baseRankFromOriginaDocs = 1;
        double originalRankPop = docSetOriginal.first()._pop;

        for (DocTuple originalTuple : docSetOriginal) {
            try {

                if (originalTuple._pop < originalRankPop) {
                    baseRankFromOriginaDocs++;
                    originalRankPop = originalTuple._pop;
                }

                StringBuilder producedTuples = new StringBuilder();

                // create a multitude of ids and tuples of records
                Map<Long, Integer> newIdsxTimes = new HashMap();
                for (int currRank = baseRankFromOriginaDocs; currRank < baseRankFromOriginaDocs + times; currRank++) {
                    long newId = generateNewID(originalTuple._id);

                    long computedPop = (long) Math.max(1, originalTuple._pop
                            * (1 / Math.pow(baseRankFromOriginaDocs, ZIPF_PARAM_S))
                            / (1 / Math.pow(currRank, ZIPF_PARAM_S)));

//                    System.err.println("computedPop=" + computedPop);
//                    System.err.println(" originalTuple._pop=" + originalTuple._pop);
//                    System.err.println("newId=" + newId);
//                    System.err.println("baseRankFromOriginaDocs=" + baseRankFromOriginaDocs);
//                    System.err.println("currRank=" + currRank);
//                    System.err.println("1 / Math.pow(baseRankFromOriginaDocs, ZIPF_PARAM_S)=" + 1 / Math.pow(baseRankFromOriginaDocs, ZIPF_PARAM_S));
//                    System.err.println("1 / Math.pow(currRank, ZIPF_PARAM_S)=" + 1 / Math.pow(currRank, ZIPF_PARAM_S));
//                    if (originalTuple._pop != computedPop) {
//                        System.exit(times);
//                    }
                    DocTuple newTuple = new DocTuple();
                    newTuple._id = newId;
                    newTuple._pop = computedPop;
                    newTuple._size = originalTuple._size;
                    newTuple._app = originalTuple._app;

                    popSum += computedPop;

                    // how many id printed in the workload for every instance of the original ID
                    int ratio = (int) Math.round((double) computedPop / originalTuple._pop);

                    newIdsxTimes.put(newId, ratio);
                    mulitpliedSet.add(newTuple);

                    producedTuples.
                            append("\t-").
                            append("@rank ").
                            append(currRank).
                            append("@ratio ").
                            append(ratio).
                            append(" ").
                            append(newTuple.toString()).
                            append("\n");

                }

                if (_lastLogged - System.currentTimeMillis() > 20000) {
                    _lastLogged = System.currentTimeMillis();
                    Logger.getLogger(TraceMultiplierApp.class.toString()).log(Level.INFO,
                            "Periodic sample logging: Finished processing original tuple: "
                            + originalTuple.toString()
                            + " which was ranked: " + baseRankFromOriginaDocs
                            + ". Produced tuples: \n" + producedTuples.toString()
                    );
                }
                // mapping between new ids and the original id
                old2newIDsxTimes.put(originalTuple._id, newIdsxTimes);

            } catch (Exception e) {
                StringWriter wtr = new StringWriter();
                PrintWriter p = new PrintWriter(wtr);
                e.printStackTrace(p);
                Logger.getLogger(TraceMultiplierApp.class.toString()).severe(wtr.toString());
//                e.printStackTrace();
//               System.exit(-1);
            }
        }

        mulitpliedSet.addAll(docSetOriginal); // dont forget to add the original ones

        for (Iterator<DocTuple> keyIt = mulitpliedSet.iterator(); keyIt.hasNext();) {
            DocTuple nextDocTuple = keyIt.next();
            docout.append(String.valueOf(nextDocTuple.get(0))).append(",");
            docout.append(String.valueOf(nextDocTuple.get(1))).append(",");
            docout.append(String.valueOf(nextDocTuple.get(2))).append(",");
            docout.append(String.valueOf(nextDocTuple.get(3))).append("\n");
        }

        //special tuple
        DocTuple specialTuple = new DocTuple();
        specialTuple._id = 0;
        specialTuple._pop = popSum;
        specialTuple._size = -1;
        specialTuple._app = -1;
        mulitpliedSet.add(specialTuple);

        return specialTuple;
    }

    private static void loadWorkload_CreateWorkloadxxCSV(
            long recs,
            final BufferedReader worksIn,
            Map<Long, Map<Long, Integer>> old2newIDsxTimes,
            final BufferedWriter workout, double videoFileSizeMultiplier) throws IOException {

        workout.append("$RECS_NUM=").append(recs + "").append("\n");

        String line;
        while ((line = worksIn.readLine()) != null) {
            StringTokenizer toks = new StringTokenizer(line, ",\t");
            try {

                //    Time of request	
                //		ID
                //		Size (in #Bytes
                Long time = (long) Double.parseDouble(toks.nextToken());
                Long originalID = Long.parseLong(toks.nextToken());
                Long size = (long) (Long.parseLong(toks.nextToken()) * videoFileSizeMultiplier);

                workout.append(String.valueOf(time)).append(",");
                workout.append(String.valueOf(originalID)).append(",");
                workout.append(String.valueOf(size)).append("\n");

                Map<Long, Integer> newIDsxTimes = old2newIDsxTimes.get(originalID);// the new "replica" ids of the original id
                if (newIDsxTimes == null) {
                    System.err.println("@ " + originalID);
                    System.err.println("@ " + originalID);
                }
                for (Long newID : newIDsxTimes.keySet()) {// this is replicated as many times as the new ids stemming from the original one
                    for (int i = 0; i < newIDsxTimes.get(newID); i++) {
                        workout.append(String.valueOf(time)).append(",");
                        workout.append(String.valueOf(newID)).append(",");
                        workout.append(String.valueOf(size)).append("\n");
                    }
                }
            } catch (IOException e) {
                Logger.getLogger(TraceMultiplierApp.class.toString()).log(Level.SEVERE, e.toString());
                e.printStackTrace();
            } catch (NumberFormatException e) {
                if (line.startsWith("$")) {
                    Logger.getLogger(TraceMultiplierApp.class.toString()).log(Level.WARNING, "ignored in trace of docs: " + line);
                } else {
                    Logger.getLogger(TraceMultiplierApp.class.toString()).log(Level.SEVERE, "line in trace of docs has issues: " + line);
                    e.printStackTrace();
                }
            }
        }
    }

    public static long generateNewID(long originalID) {
        return ++idGen + originalID;
    }

}
