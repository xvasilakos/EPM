package sim.run;

import sim.Scenario;
import app.properties.Space;
import app.properties.valid.Values;
import caching.base.AbstractCachingPolicy;
import caching.interfaces.rplc.IGainRplc;
import exceptions.CriticalFailureException;
import exceptions.InconsistencyException;
import exceptions.TraceEndedException;
import exceptions.WrongOrImproperArgumentException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import sim.space.Area;
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

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public final class TraceKolnSimulation extends SimulationBaseRunner<TraceMU> {

    private Scanner _muTraceIn;
    private String mobTrcLine;
    private String mobTrcCSVSep = " ";// default set to " "

    private Map<Integer, TraceMU> muByID;

    private Map<Integer, TraceMU> muImmobileByID;
    private Map<Integer, TraceMU> muMovingByID;
    private double muAvgVelocity;
    private static final Logger LOG = Logger.getLogger(TraceKolnSimulation.class.getName());

    private MobileGroup mobileGroup;
    private List<String> conn2SCPolicy;
    private String mobTransDecisions;

    /**
     * The simulation time which will be used as a threshold for loading the
     * next batch of trace lines
     */
    private int timeForNextBatch;
    private Collection<TraceMU> nextBatchOfMUs;
    
    /**
     * Each simulation round maps to a real time according to the trace of mobility.
     * Example: If the trace uses seconds as its time unit, then setting this 
     * variable to 3600 will map simulation rounds to hours. 
     * This has implications such as keeping statistics and results per 3600 sec
     * and so forth..
     */
    private int roundDuration = 180;

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
                mobTrcLine = _muTraceIn.nextLine();// init line
            }
        } catch (IOException ioe) {
            throw new CriticalFailureException(ioe);
        }
    }

    private void readMobilityTraceMeta() throws TraceEndedException {
        while (mobTrcLine.startsWith("#")) {
            if (mobTrcLine.toUpperCase().startsWith("#SEP=")) {
                mobTrcCSVSep = mobTrcLine.substring(5);
                LOG.log(Level.INFO, "Mobility trace uses separator=\"{0}\"", mobTrcCSVSep);
            }

            mobTraceEarlyEndingCheck();
            mobTrcLine = _muTraceIn.nextLine();// init line
        }
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
        int switched2moving = 0; // stat: num of moibiles that started to move now
        nextBatchOfMUs = new ArrayList<>();

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
            double x = Math.ceil(Double.parseDouble(csv[2]));

            //[3] y
            double y = Math.ceil(Double.parseDouble(csv[3]));

            //[4] speed
            double speed = Math.ceil(Double.parseDouble(csv[4]));

            if (!muByID.containsKey(parsedID)) {
                createMU(parsedID, (int) x, (int) y, speed);
                switched2moving++;
            } else {
                switched2moving = updateMU(parsedID, x, y, switched2moving, speed);
            }

            nextBatchOfMUs.add(this.muByID.get(parsedID));

            if (!_muTraceIn.hasNextLine()) {
                _muTraceIn.close();
                throw new TraceEndedException(trcEndStr);
            }
            mobTrcLine = _muTraceIn.nextLine();
        }

        if (getStatsHandle() != null) {
            getStatsHandle().updtSCCmpt6(switched2moving, "switched2moving");
        }

    }

    /**
     * Puts the mobile with ID=muID into the right set of mobiles, i.e. moving
     * or immobile.
     *
     * @param muID
     * @param _x
     * @param _y
     * @param switched2moving
     * @param _speed
     *
     * @return the total number of mobiles that started to move, i.e. previously
     * had a zero speed, minus the number of mobiles that stopped to move
     * (current speed is zero).
     */
    private int updateMU(int muID, double _x, double _y, int switched2moving, double _speed) {

        TraceMU nxtMU = muByID.get(muID);

        double pastSpeed = nxtMU.getSpeed();
        nxtMU.setSpeed(_speed);

        muAvgVelocity -= pastSpeed / muByID.size();
        nxtMU.setdX(_x);
        nxtMU.setdY(_y);

        muAvgVelocity += _speed / muByID.size();

        if (_speed == 0) {// if immobile in this round
            if (muMovingByID.containsKey(muID)) {// if need to change its mobility status
                muMovingByID.remove(muID);
                muImmobileByID.put(muID, nxtMU);
                switched2moving--;
            }
        } else if (muImmobileByID.containsKey(muID)) {// if it were previously immobile
            switched2moving++;
            muImmobileByID.remove(muID);
            muMovingByID.put(muID, nxtMU);
        }

        return switched2moving;
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
    protected List<TraceMU> initAndConnectMUs(
            Scenario scenario, MobileGroupsRegistry ugReg,
            Area area, CellRegistry scReg,
            Collection<AbstractCachingPolicy> cachingPolicies
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

        muByID = new HashMap();
        muImmobileByID = new HashMap();
        muMovingByID = new HashMap();

        List<TraceMU> musLst = new ArrayList<>();

        return musLst;
    }

    private TraceMU createMU(
            int muID, int x, int y, double speed) {

        TraceMUBuilder nxtMUBuilder = new TraceMUBuilder(
                this, mobileGroup, theArea.getRandPoint(),
                conn2SCPolicy, cachingStrategies, 0, 0
        );

        nxtMUBuilder.setId(muID);

        nxtMUBuilder.setArea(theArea);

        nxtMUBuilder.setTransitionDecisions(mobTransDecisions);

        TraceMU mu = nxtMUBuilder.build();

        int id = mu.getID();

        muByID.put(id, mu);
        mu.setdX(x);
        mu.setdY(y);
        muMovingByID.put(id, mu);
        muAvgVelocity += speed / muByID.size();

        if (speed > 0) {
            muMovingByID.put(id, mu);
        } else {
            muImmobileByID.put(id, mu);
        }

        return mu;
    }

    @Override
    @SuppressWarnings("empty-statement")
    public void run() {

        try {
            readMobilityTraceMeta();
            initSimClockTime();
            mobTraceEarlyEndingCheck();

            /* 
             * Load next line and extract the simulation time, 
             * which will be used as a threshold for loading the next batch 
             * of trace lines.
             */
            mobTrcLine = _muTraceIn.nextLine();
            String[] csv = mobTrcLine.split(mobTrcCSVSep);
            timeForNextBatch = Integer.parseInt(csv[0]) + roundDuration;

            int simRound = 0; // used for logging. See also var roundDuration.
            
            WHILE_THREAD_NOT_INTERUPTED:
            while (!Thread.currentThread().isInterrupted()) {
                
                int sec = simTime();
                int h = sec/3600;
                sec-=h*3600;
                
                int m = sec/60;
                sec -= m*60;
                
                
                LOG.log(Level.INFO, "Starting simulation round:{0}\n"
                        + "\tLast time loaded from mobility trace:{1}, "
                        + "which is mapped to {2}:{3}:{4} in h:min:sec",
                         
                        new Object[]{
                            (++simRound),
                            simTime(),
                            h, m, sec
                        }
                );
                
                readFromMobilityTrace();

                if (stationaryRequestsUsed()) {/*
                     * Consume data and keep gain stats for stationary users
                     */
                    for (SmallCell nxtSC : smallCells()) {
                        StationaryUser nxtSU = nxtSC.getStationaryUsr();
                        nxtSC.updtLclDmdByStationary(false);
                        nxtSU.consumeDataTry(1);
                        nxtSU.tryCacheRecentFromBH();// try to cache whatever not already in the cache that you just downloaded.
                    }

                }

/////////////////////////////////////
                _haveExitedPrevCell.clear();
                _haveHandedOver.clear();
                getStatsHandle().resetHandoverscount();

                for (TraceMU nxtMU : nextBatchOfMUs) {
                    if (!muImmobileByID.containsKey(nxtMU.getID())) {
                        nxtMU.move(false, false); // otherwise avoid expensive call to move() if possible
                    }
                    if (nxtMU.isSoftUser()) {
                        nxtMU.consumeTryAllAtOnceFromSC();
                    } else {
                        nxtMU.consumeDataTry(1);// consume in one simulation time step
                    }
                }// for all all MUs

                getStatsHandle().statHandoversCount();
/////////////////////////////////////

                for (AbstractCachingPolicy nxtPolicy : cachingStrategies) {/*
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

//                    DebugTool.printProbs(shuffldMUs.get(0).getUserGroup(), getCellRegistry());
                }
            }// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues// while simulation continues

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

    private void mobTraceEarlyEndingCheck() throws TraceEndedException {
        if (!_muTraceIn.hasNextLine()) {
            _muTraceIn.close();
            throw new TraceEndedException("The mobility trace has ended too early.");
        }
    }

    /**
     * To be called to read first line of data from the trace in order to
     * initilize the simulation clock time.
     *
     * @throws NumberFormatException
     * @throws NormalSimulationEndException
     */
    private void initSimClockTime() throws NumberFormatException, NormalSimulationEndException {
        String[] csv = mobTrcLine.split(mobTrcCSVSep);

        //[0]: time
        int time = Integer.parseInt(csv[0]);
        clock.tick(time);

        //[1] mu id
        int parsedID = Integer.parseInt(csv[1]);

        //[2] x
        double x = Math.ceil(Double.parseDouble(csv[2]));

        //[3] y
        double y = Math.ceil(Double.parseDouble(csv[3]));

        //[4] speed
        double speed = Math.ceil(Double.parseDouble(csv[4]));

        createMU(parsedID, (int) x, (int) y, speed);

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
