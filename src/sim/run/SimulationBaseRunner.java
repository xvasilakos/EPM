package sim.run;

import sim.ISimulationMember;
import sim.Scenario;
import sim.run.stats.StatsHandling;
import app.SimulatorApp;
import app.properties.Networking;
import app.properties.Simulation;
import app.properties.Space;
import app.properties.StatsProperty;
import app.properties.valid.Values;
import static app.properties.valid.Values.DISCOVER;
import caching.MaxPop;
import caching.base.AbstractCachingModel;
import caching.interfaces.rplc.IGainRplc;
import exceptions.CriticalFailureException;
import exceptions.InconsistencyException;
import exceptions.InvalidOrUnsupportedException;
import exceptions.WrongOrImproperArgumentException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.logging.Level;
import static java.util.logging.Level.INFO;
import java.util.logging.Logger;
import sim.content.Chunk;
import sim.space.Area;
import sim.content.request.DocumentRequest;
import sim.space.cell.CellRegistry;
import sim.space.connectivity.ConnectionStatusUpdate;
import sim.space.cell.MacroCell;
import sim.space.cell.smallcell.SmallCell;
import sim.time.AbstractClock;
import sim.time.NormalSimulationEndException;
import statistics.StatisticException;
import statistics.handlers.iterative.sc.cmpt6.UnonymousCompute6;
import sim.content.ContentDocument;
import sim.space.users.mobile.MobileGroupsRegistry;
import sim.space.users.mobile.MobileUser;
import traces.dmdtrace.TraceLoader;
import traces.dmdtrace.TraceWorkloadRecord;
import utilities.Couple;
import utils.CommonFunctions;
import utils.random.RandomGeneratorWrapper;
import static app.properties.Caching.CACHING__MODELS__MAXPOP_CUTTER;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 * @param <M>
 */
public abstract class SimulationBaseRunner<M extends MobileUser> implements Runnable, ISimulationMember {

    public static final ThreadGroup SIMULATION_WORKERS_GROUP = new ThreadGroup("Parallel Simulations");
    public static final Object NONE = new Object();

    protected static int _idGen = 0;
    protected final int _id = ++_idGen;
    protected final Logger LOG = CommonFunctions.getLoggerFor(this);

    protected static final Object CONCURRENT_LOCK = new Object();

    protected static int runningSimulations = 0;

    /**
     * The parameters setup for this simulation.
     */
    protected final Scenario scenarioSetup;

    protected final long _chunkSizeInBytes;
    protected final long _rateMCWlessInBytes;
    protected final long _rateSCWlessInBytes;
    protected final long _rateBHInBytes;

    /**
     * Then simulated theArea where the cells and the mobiles are placed.
     */
    protected final Area theArea;
    /**
     * A registry for the cells involved in this _sim.
     */
    protected final CellRegistry _cellRegistry;
    /**
     * A registry that contains the different groups of mobile users.
     */
    protected final MobileGroupsRegistry _musGrpsRegistry;

    protected Map<String, M> musByID;
    /**
     * The _sim "clock" which keeps the _sim simTime "ticking".
     */
    protected final AbstractClock clock;
    ///////////
    protected final StatsHandling _statsHandle;
    protected final String _decimalFormat;
    /**
     * Main caching method simulated.
     */
    protected final List<AbstractCachingModel> cachingModels;
    private final double _maxPopCachingCutter;
    protected int warmupPeriod;
    /**
     * Contains one or more entries referring to the documents of a workload.
     */
    protected List<String> _dmdTraceDocs;
    /**
     * Contains one or more entries referring to the workload of a trace,
     * corresponding to the documents in _dmdTraceDocs
     */
    protected List<String> _dmdTraceWkrlds;
    protected int _dmdTraceLimit;
    protected final boolean _shuffleWorkloadTimes;
    protected final boolean _randInitInTrace;

    // the follwing fields are used in cases of using a trace of requests demand. 
    protected int _wrkloadConsumed;
    protected int _dmdTrcReqsLoadedPerUser;
    protected TraceLoader _trcLoader;
    /**
     * A map with the loaded request records from the workload file mapped to
     * the time of request in the workload file.
     */
    protected SortedMap<Double, TraceWorkloadRecord> _wrkLoad;
    protected Map<String, ContentDocument> _trcDocs;

    /**
     * Defines how to compute popularity of items. This feature is used for
     * caching method decisions which use the popularity either to cache or to
     * replace a cached item.
     */
    protected String _itemPopCmptType;
    protected final String _overideItemSize;
    protected final int _loggingSimTimePeriod;
    protected final List<MobileUser> _haveExitedPrevCell = new ArrayList();
    protected final List<MobileUser> _haveHandedOver = new ArrayList();
    protected String theNeighborhoodType;
    private int _loadedDocumentsNum;
    private long _maxWorklaodRequestsNum;

    /////////////////////////////////////
    /////////////////////////////////////
    /////////////////////////////////////
    /**
     * Shuffles mobile users iff _sim properties impose shuffling always before
     * using mobile users; in any case, returns the list of mobile users as an
     * unmodifiable list.
     *
     * note: to deactivate shuffling with every invocation to this method, use
     * property values "never" or "upon_creation"
     *
     * @return a possibly shuffled unmodifiable list of mus.
     */
    public List<M> shuffledMUs() {
        ArrayList shuffled = new ArrayList(musByID.values());

        switch (getScenario().stringProperty(Space.MU__SHUFFLE, false)) {
            //<editor-fold defaultstate="collapsed" desc="shuffle iff property imposed">
            case Values.NEVER:
            case Values.UPON_CREATION:
                break; // do not shufle
            case Values.ALWAYS:
                Collections.shuffle(shuffled, getRandomGenerator().getMersenneTwister());
                break;
            default:
                throw new UnsupportedOperationException(
                        getScenario().stringProperty(Space.MU__SHUFFLE, false) + " not supported for " + " property " + Space.MU__SHUFFLE
                );
        }
        //</editor-fold>
        return shuffled;
    }

    @Override
    public final int simTime() {
        return clock.simTime();
    }

    @Override
    public String simTimeStr() {
        return "[" + simTime() + "]";
    }

    @Override
    public final int simID() {
        return this.getID();
    }

    /**
     * This method simply returns a reference to the current _sim itself. This
     * method is implemented as part of the corresponding interface implemented.
     *
     * @return a reference to the current _sim itself.
     */
    @Override
    public final SimulationBaseRunner getSimulation() {
        return this;
    }

    @Override
    public final CellRegistry simCellRegistry() {
        return getSimulation().getCellRegistry();
    }

    /**
     * @return the list of caching methods used.
     */
    public List<AbstractCachingModel> getCachingModels() {
        return Collections.unmodifiableList(cachingModels);
    }

    /**
     * @return the theNeighborhoodType
     */
    public String getTheNeighborhoodType() {
        return theNeighborhoodType;
    }

    /**
     * @return how many records from the workload have been loaded so far or -1
     * if no trace is used.
     */
    public int getWrkloadConsumed() {
        return usesTraceOfRequests() ? _wrkloadConsumed : -1;
    }

    /**
     * @return the percentage of records from the workload that have been loaded
     * so far or -1 if no trace is used.
     */
    public double getWrkloadConsumedPercent() {
        return usesTraceOfRequests()
                ? /*rounding like xx.yy%*/ ((int) (10000.0 * getWrkloadConsumed() / _trcLoader.getWrkloadSize())) / 100.0
                : -1;
    }

    /**
     * @return the trace loader
     */
    public TraceLoader getTrcLoader() {
        return _trcLoader;
    }

    /**
     * @return the documents of the trace
     */
    public Map<String, ContentDocument> pop() {
        return Collections.unmodifiableMap(getTrcDocs());
    }

    /**
     * @return how popularity of items must be computed.
     */
    public String getItemPopCmptType() {
        return _itemPopCmptType;
    }

    /**
     * @return the _statsHandle
     */
    public StatsHandling getStatsHandle() {
        return _statsHandle;
    }

    /**
     * @return the _decimalFormat
     */
    public String getDecimalFormat() {
        return _decimalFormat;
    }

    /**
     * @return the _trcDocs
     */
    public Map<String, ContentDocument> getTrcDocs() {
        return _trcDocs;
    }

    /**
     * @return the _wrkLoad
     */
    public SortedMap<Double, TraceWorkloadRecord> getWrkLoad() {
        return _wrkLoad;
    }

    /**
     * Does not imply number of threads in "running" state. Just the number of
     * simulations that have started to run. This number is always less than a
     * threshold.
     *
     * @return the number of currently running simulations
     */
    public static int getRunningSimulations() {
        synchronized (CONCURRENT_LOCK) {
            return runningSimulations;
        }
    }

    /**
     * Used for constructor initialization that must take place first before
     * other field assignments.
     *
     * @param s
     */
    abstract protected void constructorInit(Scenario s);

    /**
     * Private constructor. Inheriting classes must define whatever needed by
     * overriding method #SimulationBaseRunner.constructorInit()
     *
     * @param s
     * @throws CriticalFailureException
     */
    protected SimulationBaseRunner(Scenario s) throws CriticalFailureException {
        scenarioSetup = s;
        constructorInit(s);

        try {
            //<editor-fold defaultstate="collapsed" desc="init various final scenario setup parameters">

            //  initilize the clock.. //
            clock = s.initClock(this);
            int maxTime = getScenario().intProperty(app.properties.Simulation.Clock.MAX_TIME);
            int tmp = (int) (getScenario().doubleProperty(app.properties.Simulation.PROGRESS_UPDATE) * maxTime);
            _loggingSimTimePeriod = tmp == 0 ? 10 : tmp;

            // global variables for data rates
            // 125000 bytes <=> 1Mbps
//            _chunkSizeInBytes = Math.round(125000 * getSimulation().getScenario().doubleProperty(Networking.Rates.CHUNK_SIZE));
//            _rateMCWlessInBytes = (long) (125000 * scenarioSetup.doubleProperty(Networking.Rates.MC_WIRELESS));
//            _rateSCWlessInBytes = (long) (125000 * scenarioSetup.doubleProperty(Networking.Rates.SC_WIRELESS));
//            _rateBHInBytes = (long) (125000 * scenarioSetup.doubleProperty(Networking.Rates.SC_BACKHAUL));
            _chunkSizeInBytes = utils.CommonFunctions.parseSizeToBytes(
                    getSimulation().getScenario().stringProperty(
                            Networking.Rates.CHUNK_SIZE
                    )
            );
            _rateMCWlessInBytes = utils.CommonFunctions.parseSizeToBytes(
                    getSimulation().getScenario().stringProperty(
                            Networking.Rates.MC_WIRELESS
                    )
            );
            _rateSCWlessInBytes =  utils.CommonFunctions.parseSizeToBytes(
                    getSimulation().getScenario().stringProperty(
                            Networking.Rates.SC_WIRELESS
                    )
            );
            _rateBHInBytes =  utils.CommonFunctions.parseSizeToBytes(
                    getSimulation().getScenario().stringProperty(
                            Networking.Rates.SC_BACKHAUL
                    )
            );

            warmupPeriod = getScenario().intProperty(Space.SC__WARMUP_PERIOD);
            _itemPopCmptType = scenarioSetup.stringProperty(Space.ITEM__POP_CMPT, false);
            _decimalFormat = s.stringProperty(app.properties.Simulation.DecimalFormat, false);

            loadDmdTraceDocs(s);

            _overideItemSize = s.stringProperty(Space.MU__DMD__TRACE__OVERRIDE_SIZE, false);
            _dmdTraceLimit = s.intProperty(Space.MU__DMD__TRACE__LIMIT);
            _shuffleWorkloadTimes = s.isTrue(Space.MU__DMD__TRACE__SHUFFLE_WORKLOAD_TIMES);
            _randInitInTrace = s.isTrue(Space.MU__DMD__TRACE__RAND_INIT);

            if (_dmdTraceDocs.get(0).equalsIgnoreCase(Values.NONE)
                    || _dmdTraceDocs.get(0).equalsIgnoreCase(Values.NULL)
                    || _dmdTraceDocs.get(0).equalsIgnoreCase(Values.UNDEFINED)) {
                _dmdTraceDocs = null;
                _dmdTraceWkrlds = null;
            }

            _wrkLoad = new TreeMap<>();
            if (dmdTraceInUse()) {
                _trcLoader = new TraceLoader(this,
                        _dmdTraceDocs, _dmdTraceWkrlds,
                        _overideItemSize,
                        _dmdTraceLimit,
                        _randInitInTrace,
                        _shuffleWorkloadTimes
                );
                _trcDocs = _trcLoader.getDocuments();

                _dmdTrcReqsLoadedPerUser = (int) s.intProperty(Space.MU__DMD__TRACE__REQUESTS_PER_USER);
                if (_dmdTrcReqsLoadedPerUser < 0) {
                    _dmdTrcReqsLoadedPerUser = 0;
                }
            }
            //</editor-fold>
        } catch (IOException | InvalidOrUnsupportedException ex) {
            throw new CriticalFailureException(ex);
        }

        //  initilize caching methods //
        cachingModels = s.loadCachingPolicies();
        _maxPopCachingCutter = s.doubleProperty(CACHING__MODELS__MAXPOP_CUTTER);
        // initialize the theArea //
        theArea = initArea();

        // itialize cells //
        MacroCell macroCell = s.initMC(this, theArea);

        // initilize mobile users and arrange initial connectivity and proactive caching  status //
        _musGrpsRegistry = new MobileGroupsRegistry(this);

        // initialize cells and CellRegistry //
        _cellRegistry = initSCRegistry(s, _musGrpsRegistry, macroCell, theArea);
        initCellNeighborhood(_cellRegistry, s);

        // initilize statistics handling //
        _statsHandle = new StatsHandling(this);

        // initilize mobile users and arrange initial connectivity and proactive caching  status //
        musByID = initAndConnectMUs(s, _musGrpsRegistry, theArea, _cellRegistry, getCachingModels());

        _loadedDocumentsNum = 0;
        _maxWorklaodRequestsNum = 0;

        try {
            boolean preloadAll = getScenario().isTrue(
                    app.properties.Caching.CACHING__PRELOAD__CACHES__ALL__POLICIES
            );
            preCachesMostPopDocs(_cellRegistry.getSmallCells(), preloadAll);
        } catch (InvalidOrUnsupportedException | WrongOrImproperArgumentException ex) {
            throw new CriticalFailureException(ex);
        }

    }

    protected void preCachesMostPopDocs(Collection<SmallCell> scs, boolean preloadAll)
            throws WrongOrImproperArgumentException {

        if (_trcLoader == null) {
            throw new CriticalFailureException("Can not run simulations for "
                    + MaxPop.class.getCanonicalName() + " when loaded requests trace is " + null);
        }
        if (preloadAll) {
            if (!cachingModels.contains(MaxPop.instance())) {
                throw new WrongOrImproperArgumentException("Can not preload most popular"
                        + " documents in the cache for all policies without using "
                        + MaxPop.instance().toString()
                );
            }
            for (AbstractCachingModel nxtPolicy : cachingModels) {
                if (!(nxtPolicy instanceof IGainRplc)) {
                    continue;
                }
                preLoad(scs, nxtPolicy);
            }
        }

        // do it anyway for MaxPop
        if (cachingModels.contains(MaxPop.instance())) {
            preLoad(scs, MaxPop.instance()); // else do it only for maxPop
        }
    }

    protected void preLoad(Collection<SmallCell> scs, AbstractCachingModel nxtPolicy) throws CriticalFailureException {
        LOG.log(INFO, "Preloading top most popular items for policy {0}", nxtPolicy.toString());
        int count = 0;
        int num = scs.size();

        for (SmallCell nxtSC : scs) {
            SortedSet<ContentDocument> maxPopDocuments = _trcLoader.getMaxPopDocuments();
            Iterator<ContentDocument> iterator = maxPopDocuments.iterator();

            while (nxtSC.buffAvailable(nxtPolicy) > 0 && iterator.hasNext()) {
                ContentDocument nextDoc = iterator.next();
                for (Chunk nxtChunk : nextDoc.chunks()) {
                    nxtSC.initCacheAttempt(nxtPolicy, nxtChunk);
                }
            }

            if (++count % 100 == 0) {
                LOG.log(INFO, "Preloading.. {0}% completed.", Math.round(10000.0 * count / num) / 100.0);
            }
        }
    }

    protected void loadDmdTraceDocs(Scenario scenarioSetup) {

        List<String> tmp = scenarioSetup.listOfStringsProperty(Space.MU__DMD__TRACE__FILES, false);

        _dmdTraceDocs = new ArrayList();
        String pathPrefix = scenarioSetup.stringProperty(Space.MU__DMD__TRACE__DOCS_PATH, true);

        for (String nxt : tmp) {
            _dmdTraceDocs.add(pathPrefix + "/" + nxt);
        }

        _dmdTraceWkrlds = new ArrayList();
        pathPrefix = scenarioSetup.stringProperty(Space.MU__DMD__TRACE__WORKLOAD_PATH, true);
        for (String nxt : tmp) {
            _dmdTraceWkrlds.add(pathPrefix + "/" + nxt);
        }

    }

    protected boolean dmdTraceInUse() {
        return _dmdTraceDocs != null;
    }

    protected void decreaseRunningSimulations() {
        synchronized (CONCURRENT_LOCK) {
            runningSimulations--;

            LOG
                    .log(Level.INFO, "Simulation Thread {0} Ended! Currently running: {1}/{2}",
                            new Object[]{getID(), runningSimulations,
                                SimulatorApp.getMainArgs().getMaxConcurrentWorkers()
                            });
            CONCURRENT_LOCK.notifyAll();
        }
    }

    /**
     * @return the currSetup
     */
    public Scenario getScenario() {
        return scenarioSetup;
    }

    /**
     * @return the theArea
     */
    public Area getTheArea() {
        return theArea;
    }

    /**
     * @return the cellLoggersRegistry
     */
    public CellRegistry getCellRegistry() {
        return _cellRegistry;
    }

    public RandomGeneratorWrapper getRandomGenerator() {
        return getScenario().getRandomGenerator();
    }

    public MacroCell macrocell() {
        return this.getCellRegistry().getMacroCell();
    }

    public Collection<SmallCell> smallCells() {
        return this.getCellRegistry().getSmallCells();
    }

    public int getID() {
        return _id;
    }

    /**
     * Launches a sim worker-thread.
     *
     * @param setup Set of parameters that define the scenario setup
     * @return a reference to the _sim worker-thread that has just been started.
     */
    public static Couple<Thread, SimulationBaseRunner> launchSimulationThread(Scenario setup) {

        waitMaxThreadsThreshold();

        String classSimName = setup.stringProperty(Simulation.RUN__CLASS, false);

        Class simClass;
        try {
            simClass = Class.forName(classSimName);
        } catch (ClassNotFoundException ex) {
            throw new CriticalFailureException(ex);
        }

        Constructor constructor;
        try {
            constructor = simClass.getConstructor(Scenario.class);
        } catch (NoSuchMethodException | SecurityException ex) {
            throw new CriticalFailureException(ex);
        }

        SimulationBaseRunner simRnbl;
        try {
            simRnbl = (SimulationBaseRunner) constructor.newInstance(setup);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new CriticalFailureException(ex);
        }

        String simID = classSimName + "#" + String.valueOf(simRnbl.getID());
        Thread simThread = new Thread(SIMULATION_WORKERS_GROUP, simRnbl, simID);
        simThread.start();

        return new Couple<>(simThread, simRnbl);

    }

    /**
     * Wait while the maximum number of concurrently running working threads is
     * reached.
     *
     * The maximum number of concurrently running worker-threads cannot not
     * exceed the threshold defined in the main method arguments.
     *
     */
    protected static void waitMaxThreadsThreshold() {
        //<editor-fold defaultstate="collapsed" desc="enforce max concurrent workers threshold">
        synchronized (CONCURRENT_LOCK) {
            int maxConcurrentWorkers = SimulatorApp.getMainArgs().getMaxConcurrentWorkers();
            while (maxConcurrentWorkers <= runningSimulations) {
                try {
                    CONCURRENT_LOCK.wait();
                } catch (InterruptedException ex) {
                    throw new CriticalFailureException(
                            "Concurency lock for not exceeding the maximum number of workers was interrupted unxpectedly", ex);
                }
            }
            runningSimulations++;
        }
    }

    public MobileGroupsRegistry getGroupsRegistry() {
        return _musGrpsRegistry;
    }

    @Override
    public int hashCode() {
        return _id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SimulationBaseRunner other = (SimulationBaseRunner) obj;
        return this._id == other._id;
    }

    public long chunkSizeInBytes() {
        return _chunkSizeInBytes;
    }

    /**
     * @return the _rateMCWlessInBytes
     */
    public long getRateMCWlessInBytes() {
        return _rateMCWlessInBytes;
    }

    public long getRateMCWlessInChunks() {
        return Math.round(_rateMCWlessInBytes / _chunkSizeInBytes);
    }

    /**
     * @return the _rateSCWlessInBytes
     */
    public long getRateSCWlessInBytes() {
        return _rateSCWlessInBytes;
    }

    public long getRateSCWlessInChunks() {
        return Math.round(_rateSCWlessInBytes / _chunkSizeInBytes);
    }

    /**
     * @return the _rateBHInBytes
     */
    public long getRateBHInBytes() {
        return _rateBHInBytes;
    }

    public long getRateBHInChunks() {
        return Math.round(_rateBHInBytes / _chunkSizeInBytes);
    }

    public final int loggingSimTimePeriod() {
        return _loggingSimTimePeriod;
    }

    public void addHaveExitedPrevCell(MobileUser mu) {
        _haveExitedPrevCell.add(mu);
    }

    public void removeHaveExitedPrevCell(MobileUser mu) {
        _haveExitedPrevCell.remove(mu);
    }

    public void addHaveHandedOver(MobileUser mu) {
        _haveHandedOver.add(mu);
    }

    public void removeHaveHandedOver(MobileUser mu) {
        _haveHandedOver.remove(mu);
    }

    public boolean stationaryRequestsUsed() {
        return getScenario().intProperty(Space.SC__DMD__TRACE__STATIONARY_REQUESTS__RATE) > 0;
    }

    /**
     * Initializes mobile users on the area.
     *
     * @param scenario
     * @param ugReg
     * @param area
     * @param scReg
     * @param cachingPolicies
     * @return
     */
    abstract protected Map<String, M> initAndConnectMUs(
            Scenario scenario, MobileGroupsRegistry ugReg,
            Area area, CellRegistry scReg,
            Collection<AbstractCachingModel> cachingPolicies);

    protected void initCellNeighborhood(CellRegistry reg, Scenario setup) {
        theNeighborhoodType = setup.stringProperty(app.properties.Space.SC__NEIGHBORHOOD, false);
        switch (theNeighborhoodType) {
            case Values.ALL:

                for (SmallCell scI : reg.getSmallCells()) {
                    for (SmallCell scJ : reg.getSmallCells()) {
                        if (!scI.equals(scJ)) {
                            scI.addNeighbor(scJ);
                        }
                    }
                }
                break;

            case Values.ALL_PLUS_SELF:
                for (SmallCell scI : reg.getSmallCells()) {
                    for (SmallCell scJ : reg.getSmallCells()) {
                        scI.addNeighbor(scJ);
                    }
                }
                break;

            case Values.TRACE:
            case Values.DISCOVER:
            case Values.NONE:
                return;// do nothing in this case

            default:
                throw new UnsupportedOperationException(
                        "Value " + theNeighborhoodType + " "
                        + "is not supported. "
                        + "Check for wrong parameter value set for property \""
                        + app.properties.Space.SC__NEIGHBORHOOD
                        + "\""
                );

        }
    }

    protected CellRegistry initSCRegistry(Scenario s, MobileGroupsRegistry groupsRegistry,
            MacroCell mc, Area area) throws CriticalFailureException {

        try {
            CellRegistry reg = new CellRegistry(this, groupsRegistry, mc, area);
            LOG.log(Level.INFO, "Cell registry initialized.");
            return reg;
        } catch (InvalidOrUnsupportedException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new CriticalFailureException(ex);
        }
    }

    /**
     * @return the warmupPeriod
     */
    protected int getWarmupPeriod() {
        return warmupPeriod;
    }

    protected boolean runUpdtStats4SimRound() throws StatisticException {
        if (!getStatsHandle().isStatsMinTimeExceeded()) {
            return false;
        }

        getStatsHandle().updtIterativeSCCmpt4();
        getStatsHandle().updtIterativeSCCmpt4NoCachePolicy();
        getStatsHandle().updtFixedSC();
        return getStatsHandle().commitTry4Round();
    }

    /**
     * Checks if the simulation is during the warmup period, along with
     * preparing several prerequisites, such as:
     *
     * <ul>
     * <li> Statistics about newly added requests by stationaries </li>
     * <li> Loads item requests for mobiles and stationaries </li>
     * <li> Runs method runWarmUpVars() </li>
     * </ul>
     *
     * @param trcLoader the loader for the trace of user requests
     *
     * @return true if simulation time is less than the warmup period threshold.
     *
     * @throws NormalSimulationEndException
     *
     *
     * @see #runWarmUpVars()
     */
    protected boolean checkWarmupDoInitialization(TraceLoader trcLoader)
            throws NormalSimulationEndException, CriticalFailureException {

        if (simTime() <= warmupPeriod) {
            try {
                runWarmUpVars();
            } catch (InvalidOrUnsupportedException |
                    InconsistencyException | WrongOrImproperArgumentException | StatisticException ex) {
                throw new CriticalFailureException(ex);
            }
        }

        if (simTime() < warmupPeriod) {
            return true;
        } else if (simTime() == warmupPeriod) {
            /* When simTime() == warmupPeriod, reset and prepear all mobiles.
             */
            try {
                if (stationaryRequestsUsed()) {
                    for (SmallCell nxtSC : getCellRegistry().getSmallCells()) {
                        nxtSC.initLclDmdStationary();
                        nxtSC.updtLclDmdByStationary(true);
                    }
                }
            } catch (InvalidOrUnsupportedException |
                    InconsistencyException ex) {
                throw new CriticalFailureException(ex);
            }
            int newAddedReqs = 0;
            for (M nxtMU : musByID.values()) {
                if (usesTraceOfRequests()) {
                    newAddedReqs += updtLoadWorkloadRequests(nxtMU, _dmdTrcReqsLoadedPerUser);
                }
            }

            getSimulation().getStatsHandle().updtSCCmpt6(newAddedReqs,
                    new UnonymousCompute6(
                            new UnonymousCompute6.WellKnownTitle("newAddedReqs[firstTime]"))
            );
            return true;
        } else {
            return false;
        }

    }

    protected void runWarmUpVars()
            throws InvalidOrUnsupportedException,
            WrongOrImproperArgumentException, CriticalFailureException, StatisticException {

/////////////////////////////////////
        getStatsHandle().resetHandoverscount();
        _haveExitedPrevCell.clear();

        for (M nxtMU : musByID.values()) {
            ConnectionStatusUpdate updtSCConnChange = nxtMU.moveRelatively(false, true);
            if (theNeighborhoodType.equalsIgnoreCase(DISCOVER)
                    && updtSCConnChange.isHandedOver()) {
                nxtMU.getPreviouslyConnectedSC().addNeighbor(nxtMU.getCurrentlyConnectedSC());
            }
        }
        getStatsHandle().statHandoversCount();
/////////////////////////////////////

        boolean roundCommitted = runUpdtStats4SimRound();
        if (roundCommitted) {
            getStatsHandle().appendTransient(false);
            getStatsHandle().checkFlushTransient(false);
        }
    }

    //yyy keep this, only update stat code for gain which must be used
    protected void runGoldenRatioSearchEMPCLC() throws Exception {

        // checkSimEnded if needed
        if (!cachingModels.contains(caching.rplc.mingain.priced.tuned_timened.EMPC_R_Tunned_a.instance())
                || simTime() <= getSimulation().getScenario().intProperty(StatsProperty.STATS__MIN_TIME)) {
            return;
        }

        /*
       * Every once in a while, checkSimEnded for re-adjusting
       *
         */
        Collection<SmallCell> cellsRegistered = getCellRegistry().getSmallCells();
        for (SmallCell nxtSC : cellsRegistered) {
            if (simTime() % nxtSC.getEPCLCnoRplcState().getReadjustmenyPeriod() != 0) {
                break;
            }
            /* otherwise:
          *1) Re-adjust time intervals based on golden ratio search.
          *2) Then, resetGains gains maintained
             */

            SmallCell.EPCLCnoRplcState epcLCnoRplcState = nxtSC.getEPCLCnoRplcState();

            if (epcLCnoRplcState.isOptimumFound()) {
                continue;
            }

            //1
            double a = epcLCnoRplcState.getA();
            double b = epcLCnoRplcState.getB();
            double c1 = epcLCnoRplcState.getC1();
            double c2 = epcLCnoRplcState.getC2();

            double f_a = epcLCnoRplcState.getGainA();
            double f_b = epcLCnoRplcState.getGainB();
            double f_c1 = epcLCnoRplcState.getGainC1();
            double f_c2 = epcLCnoRplcState.getGainC2();

            getStatsHandle().updtSCCmpt6(a,
                    new UnonymousCompute6(
                            UnonymousCompute6.WellKnownTitle.GOLDEN_RATIO_A)
            );
            getStatsHandle().updtSCCmpt6(c1,
                    new UnonymousCompute6(
                            UnonymousCompute6.WellKnownTitle.GOLDEN_RATIO_C1)
            );
            getStatsHandle().updtSCCmpt6(c2,
                    new UnonymousCompute6(
                            UnonymousCompute6.WellKnownTitle.GOLDEN_RATIO_C2)
            );
            getStatsHandle().updtSCCmpt6(b,
                    new UnonymousCompute6(
                            UnonymousCompute6.WellKnownTitle.GOLDEN_RATIO_B)
            );

            getStatsHandle().updtSCCmpt6(f_a,
                    new UnonymousCompute6(
                            UnonymousCompute6.WellKnownTitle.GOLDEN_RATIO_F_A)
            );
            getStatsHandle().updtSCCmpt6(f_c1,
                    new UnonymousCompute6(
                            UnonymousCompute6.WellKnownTitle.GOLDEN_RATIO_F_C1)
            );
            getStatsHandle().updtSCCmpt6(f_c2,
                    new UnonymousCompute6(
                            UnonymousCompute6.WellKnownTitle.GOLDEN_RATIO_F_C2)
            );
            getStatsHandle().updtSCCmpt6(f_b,
                    new UnonymousCompute6(
                            UnonymousCompute6.WellKnownTitle.GOLDEN_RATIO_F_B)
            );

            if (f_c1 < f_c2) {// then the maximum must lie on [c1, b], so assign a = c1.
                epcLCnoRplcState.setA(c1);
                epcLCnoRplcState.recomputeC1C2();

                if (Math.abs(epcLCnoRplcState.getA() - epcLCnoRplcState.getB()) < epcLCnoRplcState.getStopE()) {
                    epcLCnoRplcState.markOptimumFound();
                }
                epcLCnoRplcState.resetGains();
            } else if (f_c1 > f_c2) { // Then the maximum must lie on [a, c2], so assign b = c2.
                epcLCnoRplcState.setB(c2);
                epcLCnoRplcState.recomputeC1C2();

                if (Math.abs(epcLCnoRplcState.getA() - epcLCnoRplcState.getB()) < epcLCnoRplcState.getStopE()) {
                    epcLCnoRplcState.markOptimumFound();
                }
                epcLCnoRplcState.resetGains();
            } // else do not reset, wait for the other round..

        }//for ever SC

    }

    /**
     *
     * @param mu
     * @param loadPerUser
     * @return
     * @throws sim.time.NormalSimulationEndException
     */
    protected int updtLoadWorkloadRequests(MobileUser mu, int loadPerUser)
            throws NormalSimulationEndException {
        int howManyToAdd = loadPerUser - mu.getRequests().size(); // just add what has finished

        int count = 0;
        do {
            loadFromWrkloadIfNeeded(loadPerUser * this.musByID.size());
            Iterator<Map.Entry<Double, TraceWorkloadRecord>> iterator = _wrkLoad.entrySet().iterator();
            while (iterator.hasNext() && howManyToAdd-- > 0) {
                TraceWorkloadRecord nxtWorkloadRecord = iterator.next().getValue();

                DocumentRequest loadedRequest = new DocumentRequest(nxtWorkloadRecord, mu);

                incrWrkloadConsumed();

                iterator.remove();
                mu.addRequest(loadedRequest);
                count++;

                //commented out to allow loading the same request ids from same mobile which resemble many mobiles during a simulation.     
                // need to also change requests collection in M to a hashset.
                //         if (nxtMU.addRequest(itemRequest)) {
                //            iterator.remove();
                //         } else {
                //            i++;// just ignore this request for this item and let it in the workload for the next mu
                //         }
            }
        } while (howManyToAdd > 0);

        mu.updtLastTimeReqsUpdt();

        return count;
    }

    public void runFinish() {

        for (AbstractCachingModel policy : getCachingModels()) {
            for (SmallCell sc : _cellRegistry.getSmallCells()) {
                sc.clearDmdPC(policy);
                sc.clearBuffer(policy);
            }
        }

        System.gc();

        try {
            LOG.log(
                    Level.INFO,
                    "Printing results for simulation {0}.",
                    new Object[]{Thread.currentThread().getName()}
            );
            getStatsHandle().prntAggregates();
            getStatsHandle().appendTransient(true);
        } catch (StatisticException ex) {
            LOG.log(Level.SEVERE, "Unsuccessful effort to print results.", ex);
        }
        decreaseRunningSimulations();
    }

    public boolean usesTraceOfRequests() {
        return _dmdTraceDocs != null;
    }

    /**
     * Loads to the workload map more records if necessary, and prints updates
     * to the user
     *
     * @param threshold
     * @throws NormalSimulationEndException in case all the requests in the
     * trace were loaded.
     */
    public void loadFromWrkloadIfNeeded(int threshold) throws NormalSimulationEndException {
        if (_wrkLoad.size() < threshold) {
            try {
                // just to be sure enough are loaded
                SortedMap<Double, TraceWorkloadRecord> loaded
                        = _trcLoader.loadFromWorkload(2 * threshold);
                _wrkLoad.putAll(loaded);
            } catch (IOException | WrongOrImproperArgumentException ex) {
                throw new CriticalFailureException(ex);
            }
        }

    }

    public void setLoadedDocumentsNum(int num) {
        _loadedDocumentsNum = num;
    }

    /**
     * @return the _loadedDocumentsNum
     */
    public int getLoadedDocumentsNum() {
        return _loadedDocumentsNum;
    }

    public void setMaxWorklaodRequestsNum(long num) {
        _maxWorklaodRequestsNum = num;
    }

    /**
     * @return the _maxWorklaodRequestsNum
     */
    public long getMaxWorklaodRequestsNum() {
        return _maxWorklaodRequestsNum;
    }

    /**
     * @param _maxWorklaodRequestsNum the _maxWorklaodRequestsNum to set
     */
    public void setMaxWorklaodRequestsNum(int _maxWorklaodRequestsNum) {
        this._maxWorklaodRequestsNum = _maxWorklaodRequestsNum;
    }

    public long incrWrkloadConsumed() {
        return _wrkloadConsumed++;
    }

    /**
     * @return the _maxPopCachingCutter
     */
    public double getMaxPopCachingCutter() {
        return _maxPopCachingCutter;
    }

    public Area initArea() throws CriticalFailureException {
        Area tmpArea = new Area(this,
                scenarioSetup.intProperty(Space.AREA__Y),
                scenarioSetup.intProperty(Space.AREA__X));

        LOG.log(Level.INFO, "{0}: {1}x{2} area; number of points={3}\n",
                new Object[]{
                    simTime(),
                    scenarioSetup.intProperty(Space.AREA__Y),
                    scenarioSetup.intProperty(Space.AREA__X),
                    tmpArea.size()
                });
        return tmpArea;
    }

}
