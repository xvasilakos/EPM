package traces.dmdtrace;

import app.SimulatorApp;
import exceptions.CriticalFailureException;
import exceptions.InconsistencyException;
import exceptions.WrongOrImproperArgumentException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import sim.run.SimulationBaseRunner;
import sim.content.ContentDocument;
import sim.space.cell.CellRegistry;
import sim.time.NormalSimulationEndException;

/**
 * Class for loading from a trace file.
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class TraceLoader implements sim.ISimulationMember {

    private final List<File> _wldTraceFiles = new ArrayList<>();
    private int _traceIdx; //indexes which trace (docs path and workload paths combination) is being used now
    private final int _howManyTraces;
    private Scanner _wrkLoadScnr;
    private final int _wrkLoadLimit;

    private final List<File> _docTraceFiles = new ArrayList<>();

    private long _totalReqNum = 0;
    private long _sumSize = 0;
    private double _stdev = -1;
    private long _overrideSizes;
    private long _maxItemSize = Long.MIN_VALUE;
    private long _minItemSize = Long.MAX_VALUE;
    /////
    private static final String META_INFO_CHAR = "$"; // used in the beggining of each workload file, for lines with meta info
// used in the beggining of each workload file, for lines with meta info
    /**
     * The number of records in the workload.
     */
    private static final String RECS_NUM = META_INFO_CHAR + "RECS_NUM";

    private int _wrkloadSize;
    private int _recsLoaded;

    /**
     * A map of information about the requested items loaded by the documents
     * file of the trace. Information is mapped to the _id of requested items.
     */
    private Map<Long, ContentDocument> _documents;
    private Long[] _documentIDs;
    private final SortedSet<ContentDocument> _maxPopInfo;
    private final SortedSet<ContentDocument> _topMaxPopInfo;
    private final Set<Long> _CDNCachedIDs; // has only the IDs of the _topMaxPopInfo sorted set
    private final SimulationBaseRunner _sim;
    private final boolean _shuffleReqTimes;
    private final boolean _randInitInTrace;

    public TraceLoader(SimulationBaseRunner sim, List<String> docsInfoPaths, List<String> workloadPaths,
            String overideSize, int wrkLoadLimit, boolean randInitInTrace, boolean shuffleReqTimes) throws FileNotFoundException, IOException {
        _wrkloadSize = 0;
        _sim = sim;
        _randInitInTrace = randInitInTrace;
        _shuffleReqTimes = shuffleReqTimes;

        _howManyTraces = docsInfoPaths.size();

        _documents = new TreeMap();

        Comparator<ContentDocument> maxPopComparator = new Comparator<ContentDocument>() {
            @Override
            public int compare(ContentDocument t1, ContentDocument t2) {
                // compares based on popularity
                int result = t2.getTotalNumberOfRequests() - t1.getTotalNumberOfRequests(); // t2 - t1 => max priority queue
//                return result == 0 ? (int) (t2.getID() - t1.getID()) // persistent choice between runs
//                        : result;
                return result == 0
                        ? _sim.getRandomGenerator().randDoubleInRange(0, 1) > 0.5 ? 1 : -1 // random choice based on seed in this simulation run
                        : result;
            }
        };

        _maxPopInfo = new TreeSet<>(maxPopComparator);
        _topMaxPopInfo = new TreeSet<>(maxPopComparator);
        _CDNCachedIDs = new HashSet<>();

        _traceIdx = 0;
        for (String nxtDocsPth : docsInfoPaths) {
            File docFile = new File(nxtDocsPth).getCanonicalFile();
            _docTraceFiles.add(docFile);
            loadFromDocs(sim, docFile,
                    utils.CommonFunctions.parseSizeToBytes(overideSize), _traceIdx);
            _traceIdx++;
            _traceIdx %= _howManyTraces;
        }

        for (String nxtWrkPth : workloadPaths) {
            _wldTraceFiles.add(new File(nxtWrkPth).getCanonicalFile());
        }
        //init the first workload stream
        _wrkLoadScnr = initWld(new FileReader(_wldTraceFiles.get(_traceIdx = 0)));
        _wrkLoadLimit = wrkLoadLimit;
    }

    /**
     * Loads all documents.
     *
     *  rdrDoc the reader for the document file.
     *  should the steam be closed after reading the contents of the file.
     */
    private void loadFromDocs(SimulationBaseRunner sim, File docFile, long overrideSize, int traceIdx) {
        int totallyLoaded = 0;

        SimulatorApp.CROSS_SIM_TRC_DOCS.acquireWriteLock();

        Map<Long, ContentDocument> crossSimDocs
                = SimulatorApp.CROSS_SIM_TRC_DOCS.getDocumentsOfTrace(docFile, this);

        if (crossSimDocs != null) {/*
             * if the documents of this file have been preloaded in the past
             */
            List<Long> meta = SimulatorApp.CROSS_SIM_TRC_DOCS.getDocumentsMeta(docFile);

            long overrid = meta.get(0);
            if (overrid != overrideSize) {
                throw new RuntimeException("Do not use multiple parameter values "
                        + "for overriding trace documents default size. It can mess up things..");

//                // if the scenario setup has a different overide_size parameter
//                SimulatorApp.CROSS_SIM_TRC_DOCS.removeDocumentsOfTrace(docFile);
//                totallyLoaded = loadFromDocs_(docFile, overrideSize, traceIdx, sim);
            } else {
                _overrideSizes = meta.get(0);
                _totalReqNum = meta.get(1);
                _maxItemSize = meta.get(2);
                _sumSize = meta.get(3);
                _minItemSize = meta.get(4);

                _documents = Collections.unmodifiableMap(crossSimDocs);
                _maxPopInfo.addAll(_documents.values());
            }
        } else {
            // if first time loading these documents
            totallyLoaded = loadFromDocs_(docFile, overrideSize, traceIdx, sim);
        }

        SimulatorApp.CROSS_SIM_TRC_DOCS.releaseWriteLock();

        arangeCDNCached();
        sim.setLoadedDocumentsNum(totallyLoaded);
        sim.setMaxWorklaodRequestsNum(_totalReqNum);
    }
//    private void loadFromDocs(SimulationBaseRunner sim, File docFile, long overrideSize, int traceIdx) {
//        int totallyLoaded = 0;
//
//        java.util.logging.Logger.getLogger(getClass().getCanonicalName()).
//                log(Level.INFO, "\n Loading documents from trace..");
//
//        try (BufferedReader br = new BufferedReader(new FileReader(docFile))) {
//            _overrideSizes = overrideSize;
//
//            String nxtLine;
//            StringTokenizer toks;
//            while ((nxtLine = br.readLine()) != null) {
//                toks = new StringTokenizer(nxtLine, ", \t\r\n");
//                String idInTrace = toks.nextToken();
//                long theID = -1;
//                try {
//                    theID = createID(idInTrace, _docTraceFiles.get(traceIdx));
//                } catch (NumberFormatException nfe) {
//                    Logger.getLogger(TraceLoader.class.getName()).log(
//                            Level.WARNING,
//                            "An exception was caght: {2}"
//                            + "line with problem is: {0}\n\t found in file {1}",
//                            new Object[]{
//                                nxtLine,
//                                docFile,
//                                nfe.getMessage()
//                            }
//                    );
//                }
//
//                int reqNum = Integer.parseInt(toks.nextToken());
//
//                _totalReqNum += reqNum;
//
//                long sizeInBytes = (long) Double.parseDouble(toks.nextToken());
//                sizeInBytes = _overrideSizes < 0 ? sizeInBytes : _overrideSizes;
//                _sumSize += sizeInBytes;
//
//                _maxItemSize = _maxItemSize < sizeInBytes ? sizeInBytes : _maxItemSize;
//                _minItemSize = _minItemSize > sizeInBytes ? sizeInBytes : _minItemSize;
//
//                int appType = Integer.parseInt(toks.nextToken());
//
//                ContentDocument nxtTrcDocument;
//                if (!SimulatorApp.CROSS_SIM_TRC_DOCS.contains(theID)) {
//                    nxtTrcDocument
//                            = new ContentDocument(theID, sim, sizeInBytes, reqNum, appType);
//                    SimulatorApp.CROSS_SIM_TRC_DOCS.addDocument(theID, nxtTrcDocument, this);
//                } else {
//                    nxtTrcDocument = SimulatorApp.CROSS_SIM_TRC_DOCS.getDocument(theID, this);
//                }
//
//                _documents.put(nxtTrcDocument.getID(), nxtTrcDocument);
//                //<editor-fold defaultstate="collapsed" desc="logging">
//                java.util.logging.Logger.getLogger(getClass().getCanonicalName()).
//                        log(Level.FINE, "\n Last document loaded id={0} (id in trace file {1})."
//                                + " The size of the described content is {2}MB"
//                                + " and it was split into {3} chunks:",
//                                new Object[]{
//                                    nxtTrcDocument.getID(),
//                                    idInTrace,
//                                    nxtTrcDocument.sizeInMBs(),
//                                    nxtTrcDocument.totalNumberOfChunks()
//                                }
//                        );
//                if (++totallyLoaded % 500 == 0) {
//                    java.util.logging.Logger.getLogger(getClass().getCanonicalName()).
//                            log(Level.INFO, "\n Totally loaded documents from trace {4}. Last document loaded id={0} (id in trace file {1})."
//                                    + " The size of the described content is {2}MB"
//                                    + " and it was split into {3} chunks:",
//                                    new Object[]{
//                                        nxtTrcDocument.getID(),
//                                        idInTrace,
//                                        nxtTrcDocument.sizeInMBs(),
//                                        nxtTrcDocument.totalNumberOfChunks(),
//                                        totallyLoaded
//                                    }
//                            );
//                }
//                //</editor-fold>
//                _maxPopInfo.add(nxtTrcDocument);
//            }
//        } catch (IOException ex) {
//            throw new CriticalFailureException(ex);
//        }
//
//        arangeCDNCached();
//
//        sim.setLoadedDocumentsNum(totallyLoaded);
//        sim.setMaxWorklaodRequestsNum(_totalReqNum);
//    }

    protected int loadFromDocs_(File docFile, long overrideSize, int traceIdx,
            SimulationBaseRunner sim) throws CriticalFailureException, NumberFormatException {

        int totallyLoaded = 0;

        java.util.logging.Logger.getLogger(getClass().getCanonicalName()).
                log(Level.INFO,
                        "\n Loading documents from trace for simulation {0}",
                        simID());
        try (BufferedReader br = new BufferedReader(new FileReader(docFile))) {
            _overrideSizes = overrideSize;

            String nxtLine;
            StringTokenizer toks;
            while ((nxtLine = br.readLine()) != null) {
                toks = new StringTokenizer(nxtLine, ", \t\r\n");
                String idInTrace = toks.nextToken();
                long theID = -1;
                try {
                    theID = createID(idInTrace, _docTraceFiles.get(traceIdx));
                } catch (NumberFormatException nfe) {
                    Logger.getLogger(TraceLoader.class.getName()).log(
                            Level.WARNING,
                            "An exception was caght: {2}"
                            + "line with problem is: {0}\n\t found in file {1}",
                            new Object[]{
                                nxtLine,
                                docFile,
                                nfe.getMessage()
                            }
                    );
                }

                int reqNum = Integer.parseInt(toks.nextToken());

                _totalReqNum += reqNum;

                long sizeInBytes = (long) Double.parseDouble(toks.nextToken());
                sizeInBytes = _overrideSizes < 0 ? sizeInBytes : _overrideSizes;
                _sumSize += sizeInBytes;

                _maxItemSize = _maxItemSize < sizeInBytes ? sizeInBytes : _maxItemSize;
                _minItemSize = _minItemSize > sizeInBytes ? sizeInBytes : _minItemSize;

                int appType = Integer.parseInt(toks.nextToken());

                ContentDocument nxtTrcDocument;
                if (!SimulatorApp.CROSS_SIM_TRC_DOCS.contains(docFile, theID)) {
                    nxtTrcDocument
                            = new ContentDocument(theID, sim, sizeInBytes, reqNum, appType);
                    SimulatorApp.CROSS_SIM_TRC_DOCS.addDocument(docFile, theID, nxtTrcDocument, this);
                } else {
                    nxtTrcDocument = SimulatorApp.CROSS_SIM_TRC_DOCS.getDocument(docFile, theID, this);
                }

                _documents.put(nxtTrcDocument.getID(), nxtTrcDocument);
                //<editor-fold defaultstate="collapsed" desc="logging">
                java.util.logging.Logger.getLogger(getClass().getCanonicalName()).
                        log(Level.FINE, "\n Last document loaded id={0} (id in trace file {1})."
                                + " The size of the described content is {2}MB"
                                + " and it was split into {3} chunks:",
                                new Object[]{
                                    nxtTrcDocument.getID(),
                                    idInTrace,
                                    nxtTrcDocument.sizeInMBs(),
                                    nxtTrcDocument.totalNumberOfChunks()
                                }
                        );
                if (++totallyLoaded % 500 == 0) {
                    java.util.logging.Logger.getLogger(getClass().getCanonicalName()).
                            log(Level.INFO, "\n Totally loaded documents from trace {4}. Last document loaded id={0} (id in trace file {1})."
                                    + " The size of the described content is {2}MB"
                                    + " and it was split into {3} chunks:",
                                    new Object[]{
                                        nxtTrcDocument.getID(),
                                        idInTrace,
                                        nxtTrcDocument.sizeInMBs(),
                                        nxtTrcDocument.totalNumberOfChunks(),
                                        totallyLoaded
                                    }
                            );
                }
                //</editor-fold>
                _maxPopInfo.add(nxtTrcDocument);
            }
        } catch (IOException ex) {
            throw new CriticalFailureException(ex);
        }

        List<Long> meta = SimulatorApp.CROSS_SIM_TRC_DOCS.getDocumentsMeta(docFile);
        if (meta == null) {
            meta = new ArrayList<>();
            SimulatorApp.CROSS_SIM_TRC_DOCS.addDocumentsMeta(docFile, meta);

        }
        meta.clear();
        meta.add(0, _overrideSizes);
        meta.add(1, _totalReqNum);
        meta.add(2, _maxItemSize);
        meta.add(3, _sumSize);
        meta.add(4, _minItemSize);

        //xxx
//        DebugTool.appendLn("\nSimulation " + simID() + " loaded first time "
//                + _documents.size() + " cross-sim cached content documents from file " + docFile.getAbsolutePath()
//                + " with signature: " + _documents.hashCode()
//                + "\n\t _overrideSizes=" + _overrideSizes
//                + "\n\t _totalReqNum=" + _totalReqNum
//                + "\n\t _maxItemSize=" + _maxItemSize
//                + "\n\t _sumSize=" + _sumSize
//                + "\n\t _minItemSize=" + _minItemSize
//        );
        return totallyLoaded;
    }

    private void arangeCDNCached() {
        double _topMaxPopPercent4CDN = _sim.getScenario().doubleProperty(
                app.properties.Space.MU__DMD__TRACE__DOCS__CDN_SERVED);
        long cdnCachedNum = Math.round(_topMaxPopPercent4CDN * _documents.size());

        // keep only the top X most popular in _topMaxPopInfo
        Iterator<ContentDocument> _maxPopInfoIter = _maxPopInfo.iterator();
        for (int i = 0; i < cdnCachedNum && _maxPopInfoIter.hasNext(); i++) {
            ContentDocument next = _maxPopInfoIter.next();
            _topMaxPopInfo.add(next);
            _CDNCachedIDs.add(next.getID());

           
            next.setCDNCached();
        }

        _documentIDs = new Long[_documents.size()];
        _documents.keySet().toArray(_documentIDs);
    }

    public long maxItemSize() {
        return _maxItemSize;
    }

    public long minItemSize() {
        return _minItemSize;
    }

    /**
     * @return the trace items in a descending popularity sorted set
     */
    public SortedSet<ContentDocument> getMaxPopDocuments() {
        return Collections.unmodifiableSortedSet(_maxPopInfo);
    }

    public Set<Long> getCDNCachedIDs() {
        return Collections.unmodifiableSet(_CDNCachedIDs);
    }

    private Scanner initWld(FileReader fileReader) throws IOException {
        Scanner scnr = new Scanner(fileReader);

        String nxtLine;
        StringTokenizer toks;

        int recordsInTrace = 500;
        while ((nxtLine = scnr.nextLine()).startsWith(META_INFO_CHAR)) {
            toks = new StringTokenizer(nxtLine, "=");
            if (toks.countTokens() > 2) {
                Logger.getLogger(getClass().getCanonicalName()).log(
                        Level.WARNING, "loaded :\"{0}\". "
                        + "All meta info workload lines must be in the "
                        + "form \"PROPERTY=VALUE\".", nxtLine);
            }

            String prop = toks.nextToken();
            String val = toks.nextToken();

            switch (prop) {
                case RECS_NUM:
                    recordsInTrace = Integer.valueOf(new StringTokenizer(val, ",\r\n\t").nextToken());
                    _wrkloadSize += recordsInTrace;
            }
        }

        if (_randInitInTrace) {
            int randInitPos = getSim().getScenario().getRandomGenerator()
                    .randIntInRange(1, recordsInTrace -20); // /-20; empirical; because some lines are alrady read..
            while (--randInitPos > 0 && scnr.hasNextLine()) {
                scnr.nextLine();//skip
            }
        }

        return scnr;
    }

    public String workLoadPaths() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (File wkrLdPath : _wldTraceFiles) {
            if (sb.length() != 0) {
                sb.append("\n");
            }
            sb.append(" - ");

            sb.append(wkrLdPath.getCanonicalPath());
        }
        return sb.toString();
    }

    @Override
    public int simTime() {
        return getSim().simTime();
    }

    @Override
    public String simTimeStr() {
        return "[" + simTime() + "]";
    }

    @Override
    public int simID() {
        return getSim().getID();
    }

    @Override
    public SimulationBaseRunner getSim() {
        return _sim;
    }

    @Override
    public CellRegistry simCellRegistry() {
        return getSim().getCellRegistry();
    }

    private static long createID(String idInTraceStr, File traceFile) {

        // do not use the whole canonical path because the doc files are in a 
        // different path than the corresponding workload files
        StringBuilder fileIDPath = new StringBuilder();
        fileIDPath.append(traceFile.getName());
        fileIDPath.append(traceFile.getParentFile().getName());
        fileIDPath.append(traceFile.getParentFile().getParentFile().getName());

        long idInTrace = Long.parseLong(idInTraceStr);

        // not necessary, but let us avoid confusion by enforcing non-negative
        long tracefileIDPathHash = Math.abs(fileIDPath.toString().hashCode());

        long hash = 17;
        hash = 31 * hash + idInTrace;
        hash = 31 * hash + tracefileIDPathHash;

        return hash;
    }

    /**
     * Each call to this method loads the next records since the last call.
     *
     * If the map returned is empty or contains less entries than the requested
     * number of record lines in the trace, then there are either no more
     * records to loadFromWorkload or the loading record limit has been reached.
     *
     *  recordLines
     * @return A map with the loaded request records from the workload file
     * mapped to the time of request in the workload file.
     * @throws IOException
     * @throws sim.time.NormalSimulationEndException
     * @throws exceptions.WrongOrImproperArgumentException
     */
    public SortedMap<Double, TraceWorkloadRecord> loadFromWorkload(int recordLines)
            throws IOException, NormalSimulationEndException, WrongOrImproperArgumentException {

        if (recordLines < 1) {
            throw new WrongOrImproperArgumentException(
                    "Can not load less than one record from workload trace"
            );
        }

        SortedMap<Double, TraceWorkloadRecord> wrkTMP = new TreeMap();

        int toLoad = recordLines;
        if (toLoad > 0) {
            if (_wrkLoadLimit > 0 && _recsLoaded >= _wrkLoadLimit) {
                throw new NormalSimulationEndException("Workload loading limit reached: "
                        + _wrkLoadLimit
                );
            }
            if (!_wrkLoadScnr.hasNextLine()) {
                while (!_wrkLoadScnr.hasNextLine()) {
                    wrkloadScannerReload();
                }
                wrkloadScannerReload();
            }
        }

        String nxtLine;

        try {
            nxtLine = _wrkLoadScnr.nextLine();
        } catch (NoSuchElementException e) {
            throw new IOException(
                    "The file at path \"" + _wldTraceFiles.get(_traceIdx) + "\""
                    + " appears to be empty, or there was an attempt to"
                    + " read from a random point in file starting at the end of the file.",
                    e
            );
        }

        while (toLoad-- > 0) {
            StringTokenizer toks;
            try {
                toks = new StringTokenizer(nxtLine, ", \t\r\n");

                double theTime;
                if (_shuffleReqTimes) {
                    theTime = getSim().getRandomGenerator().
                            randDoubleInRange(0.0, 100000/*this is big enough*/);
                    toks.nextToken();
                } else {
                    theTime = Double.parseDouble(toks.nextToken());
                    // if there is another loaded at same time, 
                    // jitter it a bit; otherwise it will replace 
                    // the other request in the loaded requests!
                    double jitter = getSim().getRandomGenerator().randDoubleInRange(0.1, 0.8);
                    while (wrkTMP.containsKey(theTime)) {
                        theTime = theTime + jitter;
//                        jitter = getSim().getRandomGenerator().randDoubleInRange(0.1, 1);
                    }
                }
                long theID = createID(//name of docs and corresponding workload files must coinside
                        toks.nextToken(), _wldTraceFiles.get(_traceIdx)
                );

                if (!_documents.containsKey(theID)) {
                    throw new InconsistencyException(theID + " is an id loaded from workload file "
                            + _wldTraceFiles.get(_traceIdx) + " but is not present in the loaded documents.");
                }

                long theSizeInBytes = _overrideSizes < 0 ? Long.parseLong(toks.nextToken()) : _overrideSizes;

                TraceWorkloadRecord rec = new TraceWorkloadRecord(getSim(), theSizeInBytes, theID, theTime);

                wrkTMP.put(rec.getTime(), rec);
                _recsLoaded++;

                if (_wrkLoadLimit > 0 && _recsLoaded >= _wrkLoadLimit) {
                    return wrkTMP;
                }

                if (!_wrkLoadScnr.hasNextLine()) {
                    while (!_wrkLoadScnr.hasNextLine()) {
                        wrkloadScannerReload();
                    }
                    nxtLine = _wrkLoadScnr.nextLine();
                }

            } catch (NumberFormatException e) {
                Logger.getLogger(getClass().getCanonicalName()).log(Level.WARNING,
                        "Erroneous record from workload ignored: {0}", nxtLine);

                if (_wrkLoadLimit > 0 && _recsLoaded >= _wrkLoadLimit) {
                    return wrkTMP;
                }

                if (!_wrkLoadScnr.hasNextLine()) {
                    while (!_wrkLoadScnr.hasNextLine()) {
                        wrkloadScannerReload();
                    }
                    nxtLine = _wrkLoadScnr.nextLine();
                }
            }

        }//while

        return wrkTMP;
    }

    private void wrkloadScannerReload() throws IOException {
        _wrkLoadScnr.close();

        int prev = _traceIdx++;
        _traceIdx %= _howManyTraces;// cyclic loadFromWorkload of traces
        if (_traceIdx != prev) {
            Logger.getLogger(getClass().getCanonicalName()).log(
                    Level.INFO,
                    "Openning next workload file: \"{0}\"", _wldTraceFiles.get(_traceIdx));
        } else {
            Logger.getLogger(getClass().getCanonicalName()).log(
                    Level.INFO,
                    "Reopenning (looping) the same workload file: \"{0}\"", _wldTraceFiles.get(_traceIdx));
        }
        _wrkLoadScnr = initWld(new FileReader(_wldTraceFiles.get(_traceIdx)));
    }

    /**
     *  itemID the _id of the item
     * @return the frequency (popularity) of request for the item according to
     * the documents file of the loaded trace.
     *
     */
    public double frequency(long itemID) {
        return numTimesRequested(itemID) / (double) _totalReqNum;
    }

    /**
     *  itemID the _id of the item
     * @return the number of times the item is requested according to the
     * documents file of the loaded trace.
     *
     */
    public int numTimesRequested(long itemID) {
        ContentDocument nfo = getDocuments().get(itemID);

        return nfo.getTotalNumberOfRequests();
    }

    /**
     * @return the average number of times an item is requested according to the
     * documents file of the loaded trace.
     */
    public double avgNumTimesRequested() {
        return (double) _totalReqNum / getDocuments().size();
    }

    /**
     * @return the average _size of times according to the documents file of the
     * loaded trace.
     */
    public double avgItemSize() {
        return (double) _sumSize / getDocuments().size();
    }

    public double stdevItemSize() {
        if (_stdev != -1) {
            return _stdev;
        }

        double sumSqrDiffs = 0;
        double avg = avgItemSize();
        Collection<ContentDocument> nfos = _documents.values();
        for (ContentDocument nxtNfo : nfos) {
            sumSqrDiffs += Math.pow(nxtNfo.sizeInBytes() - avg, 2);
        }
        return Math.sqrt(sumSqrDiffs / getDocuments().size());
    }

    /**
     * An unmodifiable map of information about the requested items loaded by
     * the documents file of the trace. Information is mapped to the _id of
     * requested items.
     *
     * @return the map of information about the requested items.
     */
    public Map<Long, ContentDocument> getDocuments() {
        return _documents;
    }

    public ContentDocument getDocument(long id) {
        return _documents.get(id);
    }

    public ContentDocument getRandomlyChosenDocument() {
        long rndID = _documentIDs[_sim.getRandomGenerator().randIntInRange(0, _documentIDs.length - 1)];
        return _documents.get(rndID);
    }

    /**
     * @return the _wrkLoadLimit
     */
    public int getWrkLoadLimit() {
        return _wrkLoadLimit;
    }

    /**
     * @return the the number of records in the workload.
     */
    public int getWrkloadSize() {
        return _wrkloadSize;
    }

    /**
     * @return the the number of records loaded sofar from the workload.
     */
    public int getRecsLoaded() {
        return _recsLoaded;
    }

    public void close() {
        _wrkLoadScnr.close();
    }

}
