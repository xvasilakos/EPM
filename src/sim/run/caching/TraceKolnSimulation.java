package sim.run.caching;

import sim.run.SimulationBaseRunner;
import sim.Scenario;
import app.properties.Space;
import caching.base.AbstractCachingModel;
import caching.interfaces.rplc.IGainRplc;
import exceptions.CriticalFailureException;
import exceptions.InconsistencyException;
import exceptions.InvalidOrUnsupportedException;
import exceptions.TraceEndedException;
import exceptions.WrongOrImproperArgumentException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import sim.space.Area;
import sim.space.Point;
import sim.space.cell.CellRegistry;
import sim.space.cell.smallcell.SmallCell;
import sim.time.NormalSimulationEndException;
import sim.space.users.StationaryUser;
import sim.space.users.mobile.MobileGroup;
import sim.space.users.mobile.MobileGroupsRegistry;
import sim.space.users.mobile.MobileUser;
import sim.space.users.mobile.TraceMU;
import sim.space.users.mobile.TraceMUBuilder;
import statistics.StatisticException;
import statistics.handlers.iterative.sc.cmpt6.UnonymousCompute6;
import traces.area.Cells;
import utilities.Couple;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public final class TraceKolnSimulation extends SimulationBaseRunner<TraceMU> {

    private Scanner _muTraceIn;
    private String mobTrcLine;
    private String mobTrcCSVSep = " ";// default set to " "

    private static final Logger LOG = Logger.getLogger(TraceKolnSimulation.class.getName());

    private MobileGroup mobileGroup;
    private List<String> conn2SCPolicy;
    private String mobTransDecisions;

    /**
     * The simulation time which will be used as a threshold for loading the
     * next batch of trace lines
     */
    private int timeForNextBatch;
    private Collection<TraceMU> batchOfMUsOfCurrRound;

    /**
     * Each simulation round maps to a real time according to the trace of
     * mobility. Example: If the trace uses seconds as its time unit, then
     * setting this variable to 3600 will map simulation rounds to hours. This
     * has implications such as keeping statistics and results per 3600 sec and
     * so forth..
     */
    private int roundDuration = 180; // 180;

    /**
     * To be defined by parsed metadata for cells' trace.
     */
    private int minX, minY, maxX, maxY, areaLengthX, areaLengthY;
    /**
     * Indicates the simulation round, based on the current simulation time
     * loaded from the mobile trace and the the round duration.
     */
    private int simRound = 0;

    public TraceKolnSimulation(Scenario s) {
        super(s);
    }

    @Override
    protected void constructorInit(Scenario scenario) {
        String mutracePath = scenario.stringProperty(Space.MU__TRACE, true);
        try {
            _muTraceIn = new Scanner(new FileReader(mutracePath));
            mobTrcLine = _muTraceIn.nextLine();// init line
            while (mobTrcLine.startsWith("#")) {
                if (mobTrcLine.toUpperCase().startsWith("#SEP=")) {
                    mobTrcCSVSep = mobTrcLine.substring(5);
                    LOG.log(Level.INFO, "Mobility trace uses separator=\"{0}\"", mobTrcCSVSep);
                }

                mobTraceEarlyEndingCheck();
                mobTrcLine = _muTraceIn.nextLine();// init line
            }
        } catch (IOException | TraceEndedException | NoSuchElementException e) {
            throw new CriticalFailureException("On attempt to load from file: "
                    + "\""
                    + mutracePath
                    + "\"", e);
        }
    }

    /**
     * Read the small cells' metadata to define minimum and maximum coordinates
     * for area dimensions.
     *
     * Using such information leads to relative dimension used in the
     * simulation.
     *
     * @return
     * @throws CriticalFailureException
     */
    @Override
    public Area initArea() throws CriticalFailureException {

        String metadataPath = scenarioSetup.stringProperty(Space.SC__TRACE_METADATA_PATH, true);
        File metaF = (new File(metadataPath)).getAbsoluteFile();
        Couple<Point, Point> areaDimensions
                = Cells.extractAreaFromMetadata(metaF, minX, minY, maxX, maxY);
        minX = areaDimensions.getFirst().getX();
        minY = areaDimensions.getFirst().getY();
        maxX = areaDimensions.getSecond().getX();
        maxY = areaDimensions.getSecond().getY();

        areaLengthX = maxX - minX;
        areaLengthY = maxY - minY;

        Area areaTmp = new Area(this, areaLengthY, areaLengthX);
        LOG.log(Level.INFO, "{0}: {1}x{2} area; number of points={3}\n",
                new Object[]{
                    simTime(),
                    0,
                    0,
                    areaTmp.size()
                });

        areaTmp.setRealAreaDimensions(minX, minY, maxX, maxY);

        return areaTmp;
    }

    /**
     * Read next lines from mobility trace until exceeding
     *
     * @throws IOException
     * @throws NumberFormatException
     * @throws InconsistencyException
     * @throws StatisticException
     * @throws TraceEndedException
     */
    private void readFromMobilityTrace() throws
            IOException, NumberFormatException,
            InconsistencyException, StatisticException,
            TraceEndedException, NormalSimulationEndException {

        String trcEndStr = "The mobility trace has ended.";
        batchOfMUsOfCurrRound = new ArrayList<>();

        while (mobTrcLine != null) {
            String[] csv = mobTrcLine.split(mobTrcCSVSep);

            //[0]: time
            int time = Integer.parseInt(csv[0]);

            if (time > this.timeForNextBatch) {
                this.timeForNextBatch = time + roundDuration;
                clock.tick(time);
                break; // in this case let it be read in the next round's batch of trace lines
            }
            //else...

            //[1] mu id
            int parsedID = Integer.parseInt(csv[1]);

            //[2] x
            int x = (int) Double.parseDouble(csv[2]) - minX; // -minX so as to be relative to area dimensions

            //[3] y
            int y = (int) Double.parseDouble(csv[3]) - minY; // -minY so as to be relative to area dimensions

            //[4] speed
            double speed = Math.ceil(Double.parseDouble(csv[4]));

            TraceMU newMU;
            if (!musByID.containsKey(parsedID)) {
                //if new mobile ID, create a new mobile user

                newMU = createMU(parsedID, x, y, speed, time);

                if (usesTraceOfRequests()) {
                    int newAddedReqs = updtLoadWorkloadRequests(newMU, _dmdTrcReqsLoadedPerUser);

                    getSimulation().getStatsHandle().updtSCCmpt6(newAddedReqs,
                            new UnonymousCompute6(
                                    new UnonymousCompute6.WellKnownTitle("newAddedReqs[firstTime]"))
                    );
                }
            } else {
                //if a known mobile ID, state its dx, dy so as to move the mobile user

                newMU = musByID.get(parsedID);
                newMU.setSpeed(speed);

                int dt = time - newMU.getTraceTime();
                newMU.setTraceTime(time);
                newMU.setDTraceTime(dt);

                int prevX = newMU.getX();
                int prevY = newMU.getY();

                newMU.setDX(x - prevX);
                newMU.setDY(y - prevY);
            }

            batchOfMUsOfCurrRound.add(newMU);


            if (!_muTraceIn.hasNextLine()) {
                _muTraceIn.close();
                throw new TraceEndedException(trcEndStr);
            }
            mobTrcLine = _muTraceIn.nextLine();
        }

    }

    /**
     * Initializes mobile users on the setArea.
     *
     * @param scenario
     * @param ugReg
     * @param area
     * @param scReg
     * @param cachingPolicies
     * @return
     */
    @Override
    protected Map<Integer, TraceMU> initAndConnectMUs(
            Scenario scenario, MobileGroupsRegistry ugReg,
            Area area, CellRegistry scReg,
            Collection<AbstractCachingModel> cachingPolicies
    ) {

        /*
         * Necessary check. With traces of user mobility, you may only have one\
         * group of users (at least in this simulator version). 
         */
        SortedMap<Integer, MobileGroup> groupsMap = ugReg.registeredGroups();
        if (groupsMap.size() != 1) {
            throw new CriticalFailureException(
                    new WrongOrImproperArgumentException(
                            "Mobile user type " + TraceMU.class.getCanonicalName()
                            + " must be used with one and only one group of mobile users."
                    ));
        }
        mobileGroup = groupsMap.get(groupsMap.firstKey());

        conn2SCPolicy = scenario.parseConnPolicySC();

        mobTransDecisions = scenario.stringProperty(Space.MU__TRANSITION_DECISIONS, false);

        musByID = new HashMap();

        return musByID;
    }

    private TraceMU createMU(
            int muID, int x, int y, double speed, int traceTime) {


        TraceMUBuilder nxtMUBuilder = new TraceMUBuilder(
                this, mobileGroup, new Point(x, y),
                conn2SCPolicy, cachingStrategies, 0, 0
        );

        nxtMUBuilder.setId(muID);

        nxtMUBuilder.setArea(theArea);

        nxtMUBuilder.setTransitionDecisions(mobTransDecisions);

        TraceMU mu = nxtMUBuilder.build();
        mu.setTraceTime(traceTime);
        mu.setDTraceTime(traceTime);

        int id = mu.getID();

        musByID.put(id, mu);

        mu.setDX(0);//dx is zero when created
        mu.setDY(0);//dy is zero when created

        mu.setSpeed(speed);

        return mu;
    }

    @Override
    @SuppressWarnings("empty-statement")
    public void run() {

        try {

            //<editor-fold defaultstate="collapsed" desc="init simulation clock time">

            /*
             * To be called to read first line of data from the trace in order to
             * initilize the simulation clock time.
             */
            String[] csv = mobTrcLine.split(mobTrcCSVSep);

//[0]: time
            int time = Integer.parseInt(csv[0]);
            clock.tick(time);

//[1] mu id
            int parsedID = Integer.parseInt(csv[1]);

//[2] x
            int x = (int) Double.parseDouble(csv[2]) - minX; // -minX so as to be relative to area dimensions

//[3] y
            int y = (int) Double.parseDouble(csv[3]) - minY; // -minY so as to be relative to area dimensions

//[4] speed
            double speed = Math.ceil(Double.parseDouble(csv[4]));

            TraceMU newMU = createMU(parsedID, x, y, speed, time);

            if (usesTraceOfRequests()) {
                int newAddedReqs = updtLoadWorkloadRequests(newMU, _dmdTrcReqsLoadedPerUser);

                getSimulation().getStatsHandle().updtSCCmpt6(newAddedReqs,
                        new UnonymousCompute6(
                                new UnonymousCompute6.WellKnownTitle("newAddedReqs[firstTime]"))
                );
            }
//</editor-fold>

            //<editor-fold defaultstate="collapsed" desc="init stationaries' demand">
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
//</editor-fold>

            // usefull for stationary ammount of data concumption 
            int trackClockTime = simTime();

            /* 
             * Load next line and extract the simulation time, 
             * which will be used as a threshold for loading the next batch 
             * of trace lines.
             */
            mobTrcLine = _muTraceIn.nextLine();
            csv = mobTrcLine.split(mobTrcCSVSep);
            timeForNextBatch = Integer.parseInt(csv[0]) + roundDuration;

            simRound = 0; // used for logging. See also var roundDuration.

            WHILE_THREAD_NOT_INTERUPTED:
            while (!Thread.currentThread().isInterrupted()) {

                logSimulationRound();

                readFromMobilityTrace();
                int roundTimeSpan = simTime() - trackClockTime; // in time units specified by the trace
                trackClockTime = simTime();

                if (stationaryRequestsUsed()) {

                    /*
                     * Consume data and keep gain stats for stationary users
                     */
                    for (SmallCell nxtSC : smallCells()) {
                        StationaryUser nxtSU = nxtSC.getStationaryUsr();
                        nxtSC.updtLclDmdByStationary(false);
                        nxtSU.consumeDataTry(roundTimeSpan);
                        nxtSU.tryCacheRecentFromBH();// try to cache whatever not already in the cache that you just downloaded.
                    }

                }

                /////////////////////////////////////
                _haveExitedPrevCell.clear();
                _haveHandedOver.clear();
                getStatsHandle().resetHandoverscount();
                for (TraceMU nxtMU : batchOfMUsOfCurrRound) {
                    if (nxtMU.getSpeed() > 0.0) {
                        nxtMU.moveRelatively(); // otherwise avoid expensive call to moveRelatively() if possible
                    }
                    if (nxtMU.isSoftUser()) {
                        nxtMU.consumeTryAllAtOnceFromSC();
                    } else {
                        nxtMU.consumeDataTry(
                                // consume based on time span since last move for user
                                nxtMU.getdTraceTime()
                        );
                    }
                }// for all all MUs

                getStatsHandle().statHandoversCount();
/////////////////////////////////////

                for (AbstractCachingModel nxtPolicy : cachingStrategies) {/*
                     * update priority queues of cached chunks for each
                     * IGainRplc replacement policy, in every small cell.
                     */
                    if (!(nxtPolicy instanceof IGainRplc)) {
                        continue;
                    }
                    IGainRplc gainRplcPolicy = (IGainRplc) nxtPolicy;
                    for (SmallCell sc : smallCells()) {
                        sc.updtCachedChunksOrderedByGain(gainRplcPolicy);
                    }
                }

                for (MobileUser nxtMU : _haveHandedOver) {/*
                     * can consume after handover
                     */
                    nxtMU.setReqsConsumeReady();
                }

                /////////////////////////////////////////////////    
                int clearedReqs = 0;
                int newAddedReqs = 0;
                for (MobileUser nxtMU : _haveExitedPrevCell) {

                    SmallCell lastSCForCacheDecisions = nxtMU.getLastSCForCacheDecisions();
                    if (lastSCForCacheDecisions != null) {
                        getStatsHandle().updtPerformanceStats(nxtMU);
                        // cancel past PC decisions
                        nxtMU.cancelAndDeregisterPCOrders();
                    }

                    if (usesTraceOfRequests()) {
                        clearedReqs += nxtMU.clearCompletedRequests();
                        newAddedReqs += updtLoadWorkloadRequests(nxtMU, _dmdTrcReqsLoadedPerUser);
                    }

                    // finaly take caching decisions
                    nxtMU.cacheDescisionsPerformRegisterPC(nxtMU.getLastKnownConnectedSC());
                }
                getStatsHandle().updtSCCmpt6(clearedReqs,
                        new UnonymousCompute6(
                                new UnonymousCompute6.WellKnownTitle("ClearedReqs"))
                );
                getStatsHandle().updtSCCmpt6(newAddedReqs,
                        new UnonymousCompute6(
                                new UnonymousCompute6.WellKnownTitle("NewReqs"))
                );

                ////////////////////////////////////////////////////
                boolean roundCommited = runUpdtStats4SimRound();
                if (roundCommited) {
                    getStatsHandle().appendTransient(false);
                    getStatsHandle().checkFlushTransient(false);
                }
            }// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues

        } catch (NormalSimulationEndException simEndEx) {
            LOG.log(Level.INFO, "Simulation {0} ended: {1}",
                    new Object[]{
                        Thread.currentThread().getName(),
                        simEndEx.getMessage()
                    });
        } catch (StatisticException ex) {
            LOG.log(Level.SEVERE, "Simulation " + getID()
                    + " terminates unsuccessfully at time " + simTime(),
                    new CriticalFailureException(ex));
        } catch (Throwable ex) {
            LOG.log(Level.SEVERE, "Simulation " + getID()
                    + " terminates unsuccessfully at time " + simTime(),
                    new CriticalFailureException(ex));
        } finally {
            runFinish();
        }
    }

    private void logSimulationRound() {
        int sec = simTime();
        int h = sec / 3600;
        sec -= h * 3600;

        int m = sec / 60;
        sec -= m * 60;

        if (simRound < 101 && ++simRound % 10 == 0
                || simRound < 1001 && ++simRound % 100 == 0) {
            LOG.log(Level.INFO, "Begining simulation round:{0}\n"
                    + "\tLast time loaded from mobility trace:{1}, "
                    + "which is mapped to {2}:{3}:{4} in h:min:sec",
                    new Object[]{
                        simRound,
                        simTime(),
                        h, m, sec
                    }
            );
        }
    }

    private void mobTraceEarlyEndingCheck() throws TraceEndedException {
        if (!_muTraceIn.hasNextLine()) {
            _muTraceIn.close();
            throw new TraceEndedException("The mobility trace has ended too early.");
        }
    }

    @Override
    protected boolean runUpdtStats4SimRound() throws StatisticException {
        if (!getStatsHandle().isStatsMinTimeExceeded()) {
            return false;
        }
//yyy        getStatsHandle().updtSCCmpt6(
//                _muAvgVelocity,
//                new UnonymousCompute6(
//                        new UnonymousCompute6.WellKnownTitle("AVGSpeed")
//                )
//        );
//        getStatsHandle().updtSCCmpt6(
//                _muAvgVelocity * _muByID.size() / _muMovingByID.size(),
//                new UnonymousCompute6(
//                        new UnonymousCompute6.WellKnownTitle("AVGSpeed[no immobile]")
//                )
//        );
//        getStatsHandle().updtSCCmpt6(
//                _muImmobileByID.size(),
//                new UnonymousCompute6(
//                        new UnonymousCompute6.WellKnownTitle("n_Immobile")
//                )
//        );

        return super.runUpdtStats4SimRound();
    }

    @Override
    public void runFinish() {
        super.runFinish();
        _trcLoader.close();
        _muTraceIn.close();
    }

}
