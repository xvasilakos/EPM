package sim.run.multicast;

import app.properties.Simulation;
import sim.run.SimulationBaseRunner;
import sim.Scenario;
import app.properties.Space;
import app.properties.StatsProperty;
import static app.properties.valid.Values.DISCOVER;
import caching.base.AbstractCachingModel;
import caching.interfaces.rplc.IGainRplc;
import exceptions.CriticalFailureException;
import exceptions.InconsistencyException;
import exceptions.InvalidOrUnsupportedException;
import exceptions.TraceEndedException;
import exceptions.WrongOrImproperArgumentException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import sim.space.Area;
import sim.space.Point;
import sim.space.cell.CellRegistry;
import sim.space.cell.smallcell.SmallCell;
import sim.space.connectivity.ConnectionStatusUpdate;
import sim.time.NormalSimulationEndException;
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
 * @author Xenofon Vasilakos ({@literal xvas@aueb.gr} - mm.aueb.gr/~xvas),
 * Mobile Multimedia Laboratory (mm.aueb.gr), Dept. of Informatics, School of
 * Information {@literal Sciences & Technology}, Athens University of Economics
 * and Business, Greece
 */
public final class MulticastKolnSimulation extends SimulationBaseRunner<TraceMU> {

    private String muTracePath;
    private BufferedReader muTraceBf;
    private String mobTrcLine;
    private String mobTrcCSVSep = " ";// default set to " "

    private static final Logger LOG = Logger.getLogger(MulticastKolnSimulation.class.getName());

    private MobileGroup mobileGroup;
    private List<String> conn2SCPolicy;
    private String mobTransDecisions;

    private int currBatchVBegin;

    /**
     * The simulation time which will be used as a threshold for loading the
     * next batch of trace lines
     */
    private int timeForNextBatch;
    private Collection<TraceMU> batchOfMUsOfCurrRound;

    private int roundDuration;

    /**
     * To be defined by parsed metadata for cells' trace.
     */
    private int minX, minY, maxX, maxY, areaLengthX, areaLengthY;
    /**
     * Indicates the simulation round, based on the current simulation time
     * loaded from the mobile trace and the the round duration.
     */
    private int simRound;
    private int linesReadFromMob;

    public MulticastKolnSimulation(Scenario s) {
        super(s);
    }

    @Override
    protected void init(Scenario scenario) {
        muTracePath = scenario.stringProperty(Space.MU__TRACE, true);
        roundDuration = getScenario().intProperty(StatsProperty.STATS__AGGREGATES__RECORDING_PERIOD);

        try {
            this.muTraceBf = new BufferedReader(new FileReader(muTracePath));

            while (null != (mobTrcLine = muTraceBf.readLine())) {
                linesReadFromMob++;

                if (mobTrcLine.startsWith("#")) {
                    if (mobTrcLine.toUpperCase().startsWith("#SEP=")) {
                        mobTrcCSVSep = mobTrcLine.substring(5);
                        LOG.log(Level.INFO, "Mobility trace uses separator=\"{0}\"", mobTrcCSVSep);
                    }
                } else {
                    break;
                }
            }

            if (mobTrcCSVSep == null) {
                mobTrcCSVSep = " ";
            }

            if (mobTrcLine == null) {
                muTraceBf.close();
                String trcEndStr = "The mobility trace has ended too early"
                        + "(after initilisation):"
                        + "\n\t- path:"
                        + "\""
                        + muTracePath
                        + "\""
                        + "\n\t- trace line read:"
                        + "\""
                        + mobTrcCSVSep
                        + "\"";
                throw new TraceEndedException(trcEndStr);
            }

            /*
             * To be called to read first line of data from the trace in order to
             * initilize the simulation clock time.
             */
            int initTime = scenario.intProperty(Simulation.Clock.INIT_TIME);
            String[] csv = mobTrcLine.split(mobTrcCSVSep);
            int time = Integer.parseInt(csv[0]);

            if (time < initTime) {// do not start unless a minimum time has been reached.
                while (null != (mobTrcLine = muTraceBf.readLine()) && time < initTime) {
                    csv = mobTrcLine.split(mobTrcCSVSep);
                    time = Integer.parseInt(csv[0]);
                    linesReadFromMob++;
                }
            }

            if (mobTrcLine == null) {
                muTraceBf.close();
                String trcEndStr = "The mobility trace has ended too early"
                        + "(after initilisation):"
                        + "\n\t- path:"
                        + "\""
                        + muTracePath
                        + "\""
                        + "\n\t- trace line read:"
                        + "\""
                        + mobTrcCSVSep
                        + "\"";
                throw new TraceEndedException(trcEndStr);
            }

            updtClockAndBatchTimes(time);

            simRound = 0; // used for logging. See also var roundDuration.
            linesReadFromMob = 0;

        } catch (IOException | TraceEndedException | NoSuchElementException e) {
            throw new CriticalFailureException("On attempt to load from file: "
                    + "\""
                    + muTracePath
                    + "\"", e);
        } catch (NormalSimulationEndException ex) {
            String trcEndStr = "Clock time ended too early: " + simTimeStr()
                    + " while initialising from mobility trace \""
                    + muTracePath
                    + "\""
                    + "\n\t- trace line read:"
                    + "\""
                    + mobTrcCSVSep
                    + "\"";
            throw new CriticalFailureException(trcEndStr);
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

        String metadataPath = scenarioSetup.stringProperty(Space.SC__TRACE_BASE, true)
                + "/" + scenarioSetup.stringProperty(Space.SC__TRACE_METADATA, true);

        File metaF = (new File(metadataPath)).getAbsoluteFile();
        Couple<Point, Point> areaDimensions
                = Cells.extractAreaFromMetadata(metaF);
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
     * @return true if the mobility trace has ended.
     *
     * @throws IOException
     * @throws NumberFormatException
     * @throws InconsistencyException
     * @throws StatisticException
     * @throws TraceEndedException
     */
    private boolean readFromMobilityTrace() throws
            IOException, NumberFormatException,
            InconsistencyException, StatisticException,
            TraceEndedException, NormalSimulationEndException {

        batchOfMUsOfCurrRound = new ArrayList<>();

        //System.err.println("** last read: " + mobTrcLine);
        do {

            String[] csv = mobTrcLine.split(mobTrcCSVSep);

            //[0]: time
            int time = Integer.parseInt(csv[0]);

            if (time >= this.timeForNextBatch) {
                updtClockAndBatchTimes(time);
                return false; // in this case let it be read in the next round's batch of trace lines
            }
            //else...

            //[1] mu id
            String parsedID = csv[1];

//            //[2] x
//            int x = (int) Double.parseDouble(csv[2]) - minX; // -minX so as to be relative to area dimensions
//
//            //[3] y
//            int y = (int) Double.parseDouble(csv[3]) - minY; // -minY so as to be relative to area dimensions
//TODO
//BUGFIX
//hack using max for out of area coordinates. 
// somehow the trace of mobiliy has out of bounds moves:
            //[2] x
            int x = (int) Math.max(0, Double.parseDouble(csv[2]) - minX); // -minX so as to be relative to area dimensions

            //[3] y
            int y = (int) Math.max(0, Double.parseDouble(csv[3]) - minY); // -minY so as to be relative to area dimensions

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
                                    new UnonymousCompute6.WellKnownTitle("NewDemand"))
                    );
                    getSimulation().getStatsHandle().updtSCCmpt6(newMU.getRequests().size(),
                            new UnonymousCompute6(
                                    new UnonymousCompute6.WellKnownTitle("TotalDemand"))
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

            linesReadFromMob++;
        } while (null != (mobTrcLine = muTraceBf.readLine()));

        return true; // if reached here, then the mobility trace is over

    }

    /**
     * Sets the time of next batch of mobile user moves and the clock time to an
     * Integer multiple of #roundDuration.
     *
     * @param time the last time parsed from mobility trace
     * @throws sim.time.NormalSimulationEndException
     */
    protected void updtClockAndBatchTimes(int time) throws NormalSimulationEndException {
        int tmp = (time + roundDuration);
        int ypoloipo = tmp % roundDuration;
        timeForNextBatch = tmp - ypoloipo;
        currBatchVBegin = timeForNextBatch - roundDuration;

        clock.tickAllowBackwardsTime(currBatchVBegin);
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
    protected Map<String, TraceMU> initAndConnectMUs(
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
            String muID, int x, int y, double speed, int traceTime) {

        TraceMUBuilder nxtMUBuilder = new TraceMUBuilder(
                this, mobileGroup, new Point(x, y),
                conn2SCPolicy, cachingModels, 0, 0
        );

        nxtMUBuilder.setId(muID);

        nxtMUBuilder.setArea(theArea);

        nxtMUBuilder.setTransitionDecisions(mobTransDecisions);

        TraceMU mu = nxtMUBuilder.build();
        mu.setTraceTime(traceTime);
        mu.setDTraceTime(traceTime);

        String id = mu.getID();

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

            boolean mobTraceFinished = false;

            //<editor-fold defaultstate="collapsed" desc="init stationaries' demand">
//todo TMPORARILY NO STATIONARIES            try {
//                if (stationaryRequestsUsed()) {
//                    for (SmallCell nxtSC : getCellRegistry().getSmallCells()) {
//                        nxtSC.initLclDmdStationary();
//                        nxtSC.updtLclDmdByStationary(true);
//                    }
//                }
//            } catch (InvalidOrUnsupportedException |
//                    InconsistencyException ex) {
//                throw new CriticalFailureException(ex);
//            }
            //</editor-fold>
//            // usefull for stationary ammount of data concumption 
//TODO            int trackClockTime = simTime();
            WHILE_THREAD_NOT_INTERUPTED:
            while (!Thread.currentThread().isInterrupted()) {

                if (mobTraceFinished) {
                    throw new NormalSimulationEndException("Mobility trace has been fully read. #Lines read: " + linesReadFromMob);
                }

                logSimulationRound();

                mobTraceFinished = readFromMobilityTrace();

//TODO commented to lighten the sim..
//                trackClockTime = simTime();
//                if (stationaryRequestsUsed()) {
//                int roundTimeSpan = simTime() - trackClockTime; // in time units specified by the trace
//                    /*
//                     * Consume data and keep gain stats for stationary users
//                     */
//                    for (SmallCell nxtSC : smallCells()) {
//                        StationaryUser nxtSU = nxtSC.getStationaryUsr();
//                        nxtSC.updtLclDmdByStationary(false);
//                        nxtSU.consumeDataTry(roundTimeSpan);//TODO
//                        nxtSU.tryCacheRecentFromBH();// try to cache whatever not already in the cache that you just downloaded.
//                    }
//
//                }
////////////////////////////////////////////////////////////////////////////////
//////////////////////////// mobiles move //////////////////////////////////////
                _haveExitedPrevCell.clear();
                _haveHandedOver.clear();
                getStatsHandle().resetHandoverscount();
                for (TraceMU nxtMU : batchOfMUsOfCurrRound) {
                    if (nxtMU.getSpeed() > 0.0) {
                        ConnectionStatusUpdate updtSCConnChange = nxtMU.moveRelatively();

                        //TODO reconsider this... added only to run example codes
                        if (theNeighborhoodType.equalsIgnoreCase(DISCOVER)
                                && updtSCConnChange.isHandedOver()) {
                            nxtMU.getPreviouslyConnectedSC().addNeighbor(nxtMU.getCurrentlyConnectedSC());
                        }

                    }
                    if (nxtMU.isSoftUser()) {
                        nxtMU.consumeSftUsr();//TODO
                    } else {
                        nxtMU.consumeHardUsr(//TODO
                                // consume based on time span since last move for user
                                nxtMU.getdTraceTime()
                        );
                    }
                }// for all all MUs

                getStatsHandle().statHandoversCount();
//////////////////////////// mobiles move //////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////                

                for (AbstractCachingModel nxtPolicy : cachingModels) {//TODO
                    /*
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

                    SmallCell lastSCForCacheDecisions = nxtMU.getLastSCForCacheDecisions();//TODO
                    if (lastSCForCacheDecisions != null) {
                        getStatsHandle().updtPerformanceStats(nxtMU);
                        // cancel past PC decisions
                        nxtMU.cancelAndDeregisterPCOrders();//TODO
                    }

                    if (usesTraceOfRequests()) {
                        clearedReqs += nxtMU.clearCompletedRequests();
                        newAddedReqs += updtLoadWorkloadRequests(nxtMU, _dmdTrcReqsLoadedPerUser);
                    }

                    // finaly take caching decisions
                    nxtMU.cacheDescisionsPerformRegisterPC(nxtMU.getLastKnownConnectedSC());//TODO
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
                /*
                 * Update the statistics for this round. Unlike its base class 
                 * implementation, it tries to commit the statistics without 
                 * checking the simulation time.
                 */
                getStatsHandle().updtIterativeSCCmpt4();
                getStatsHandle().updtIterativeSCCmpt4NoCachePolicy();
                getStatsHandle().updtFixedSC();
                getStatsHandle().commitForce4Round(this.roundDuration);

                getStatsHandle().appendTransient(false);
                getStatsHandle().checkFlushTransient(false);

            }// while simulation continues

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

    @Override
    public void runFinish() {
        super.runFinish();
        _trcLoader.close();
        try {
            muTraceBf.close();
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

}
