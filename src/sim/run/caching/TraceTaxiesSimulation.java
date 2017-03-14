package sim.run.caching;

import sim.run.SimulationBaseRunner;
import sim.Scenario;
import app.properties.Space;
import app.properties.valid.Values;
import caching.base.AbstractCachingPolicy;
import caching.interfaces.rplc.IGainRplc;
import exceptions.CriticalFailureException;
import exceptions.InconsistencyException;
import exceptions.ScenarioSetupException;
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
public final class TraceTaxiesSimulation extends SimulationBaseRunner<TraceMU> {

    private Scanner _muTraceIn;
    private String mobTrcLine;
    private String mobTrcCSVSep = " ";// default set to " "

    private Map<Integer, TraceMU> _muByID;
    /**
     * Original number of mobiles, not including clone mobiles
     */
    private int _totalOriginalMUsNum;
    private Map<Integer, TraceMU> _muImmobileByID;
    private Map<Integer, TraceMU> _muMovingByID;
    private int _cloneMobsFactor;
    private double _muAvgVelocity;

    public TraceTaxiesSimulation(Scenario s) {
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

    private void updateTraceMU() throws IOException, NumberFormatException,
            InconsistencyException, StatisticException, TraceEndedException {
        int simTime = clock.simTime();
        int switched2moving = 0;

        String trcEndStr = "The mobility trace has ended.";

        while (mobTrcLine != null) {
            String[] csv = mobTrcLine.split(mobTrcCSVSep);
            if (csv[0].startsWith("#")) {
                if (!_muTraceIn.hasNextLine()) {
                    _muTraceIn.close();
                    throw new TraceEndedException(trcEndStr);
                }
                mobTrcLine = _muTraceIn.nextLine();
                continue;
            }

            int time = Integer.parseInt(csv[0]);
            int parsedMUID = Integer.parseInt(csv[1]);
            double dxdt = Math.ceil(Double.parseDouble(csv[2]));
            double dydt = Math.ceil(Double.parseDouble(csv[3]));

            switched2moving = updateTraceMUNxtMU(parsedMUID, dxdt, dydt, switched2moving);

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

    private int updateTraceMUNxtMU(int originalID, double dxdt, double dydt, int switched2moving) {

        List<Integer> cloneIDs = cloneIDs(_cloneMobsFactor, _totalOriginalMUsNum, originalID);

        cloneIDs.add(0, originalID);

        for (Integer nxtID : cloneIDs) {
            TraceMU nxtMU = _muByID.get(nxtID);

            double prevDx = nxtMU.getdX();
            double prevDy = nxtMU.getdY();

            double velocity = Math.sqrt(prevDx * prevDx + prevDy * prevDy);

            _muAvgVelocity -= velocity / _muByID.size();
            nxtMU.setDX(dxdt);
            nxtMU.setDY(dydt);

            double newVelocity = Math.sqrt(dxdt * dxdt + dydt * dydt);

            _muAvgVelocity += newVelocity / _muByID.size();

            if (dxdt == dydt && dxdt == 0) {// if immobile in this round
                if (_muMovingByID.containsKey(nxtID)) {// if need to change its mobility status
                    _muMovingByID.remove(nxtID);
                    _muImmobileByID.put(nxtID, nxtMU);
                }
            } else if (_muImmobileByID.containsKey(nxtID)) {// if it were previously immobile
                switched2moving++;
                _muImmobileByID.remove(nxtID);
                _muMovingByID.put(nxtID, nxtMU);
            }

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
    protected Map<Integer, TraceMU> initAndConnectMUs(
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
        MobileGroup nxtGroup = groupsMap.get(groupsMap.firstKey());

        List<String> conn2SCPolicy;
        conn2SCPolicy = scenario.parseConnPolicySC();
        String mobTransDecisions = scenario.stringProperty(Space.MU__TRANSITION_DECISIONS, false);
        double percentage = scenario.doubleProperty(app.properties.Simulation.PROGRESS_UPDATE);

        _muByID = new HashMap();
        _muImmobileByID = new HashMap();
        _muMovingByID = new HashMap();

        Map<Integer, TraceMU> musByTheirID = new HashMap<>();

        String metaPath = scenario.stringProperty(Space.MU__TRACE__META, true);

        if (!metaPath.equalsIgnoreCase(Values.NONE)) {
            initAndConnectMUs_1(metaPath, percentage, area, nxtGroup,
                    conn2SCPolicy, cachingPolicies, mobTransDecisions,
                    new ArrayList(musByTheirID.values()));
        } else {// Makes a first full parse of the mobility trace file to discover meta data.
            initAndConnectMUs_2(percentage, area, nxtGroup,
                    conn2SCPolicy, cachingPolicies, mobTransDecisions,
                    new ArrayList(musByTheirID.values()), scenario);
        }

        // do the cloning or reduction of MUs
        _totalOriginalMUsNum = _muByID.size();
        if (_cloneMobsFactor != 0) {
            muCloning(new ArrayList(musByTheirID.values()), area, nxtGroup,
                    conn2SCPolicy, cachingPolicies, mobTransDecisions);
        }

        return musByTheirID;
    }

    private void muCloning(List<TraceMU> musLst, Area area, MobileGroup nxtGroup, List<String> conn2SCPolicy, Collection<AbstractCachingPolicy> cachingPolicies, String mobTransDecisions) throws CriticalFailureException {
        try {
            _cloneMobsFactor = scenarioSetup.intProperty(Space.MU__CLONEFACTOR);

            if (_cloneMobsFactor < -1) {
                throw new exceptions.ScenarioSetupException(
                        Space.MU__CLONEFACTOR.toString()
                        + " can be only withing [-1, infinity]"
                );
            } else if (_cloneMobsFactor > 0) {
                cloneMUs(musLst, area, nxtGroup, conn2SCPolicy, cachingPolicies,
                        mobTransDecisions, _cloneMobsFactor, _totalOriginalMUsNum);
            } else if (_cloneMobsFactor == -1) {
                musLst.clear();
            } else {
                //reduce users
                int rmvCounter = (int) Math.abs(musLst.size() * _cloneMobsFactor);
                while (rmvCounter > 0 && !musLst.isEmpty()) {
                    int rand = getSimulation().getRandomGenerator().
                            randIntInRange(0, musLst.size() - 1);
                    musLst.remove(rand);
                }
            }
        } catch (ScenarioSetupException scnEx) {
            throw new exceptions.CriticalFailureException(scnEx);
        }
    }

    private void cloneMUs(List<TraceMU> musLst, Area area, MobileGroup nxtGroup,
            List<String> conn2SCPolicy,
            Collection<AbstractCachingPolicy> cachingPolicies,
            String mobTransDecisions, int cloneMobsFactor, int totalMUsNum) {

        List<TraceMU> originalMUs = new ArrayList(musLst);
        /**
         * use tmp to avoid concurrent modification
         */

        for (TraceMU mu : originalMUs) {
            int originalID = mu.getID();
            List<Integer> cloneIDs = cloneIDs(cloneMobsFactor, totalMUsNum, originalID);

            for (Integer nxtID : cloneIDs) {
                createTraceMU(nxtGroup, conn2SCPolicy,
                        cachingPolicies, nxtID,
                        area,
                        mobTransDecisions, musLst);

            }
        }

        LOG.info(cloneMobsFactor + " clones per original mobile added.");
    }

    private List<Integer> cloneIDs(int cloneMobsFactor, int totalMUsNum, int originalID) {
        List<Integer> cloneIDs = new ArrayList();

        int count = 0;
        while (++count <= cloneMobsFactor) {
            int cloneID = count * totalMUsNum + originalID;
            cloneIDs.add(cloneID);
        }

        return cloneIDs;
    }

    private void initAndConnectMUs_1(String metaDataPath,
            double percentage, Area area, MobileGroup nxtGroup,
            List<String> conn2SCPolicy, Collection<AbstractCachingPolicy> cachingPolicies,
            String mobTransDecisions, List<TraceMU> musLst)
            throws CriticalFailureException, InconsistencyException, NumberFormatException {

        String lineCSV, sep = " ";
        try (BufferedReader metaIn = new BufferedReader(new FileReader(metaDataPath))) {
            int musNum = -1;

            while ((lineCSV = metaIn.readLine()) != null) {

                if (lineCSV.toUpperCase().startsWith("#NUM=")) {
                    String[] csv = lineCSV.split("=");
                    musNum = Integer.parseInt(csv[1]);
                    continue;
                }

                if (lineCSV.toUpperCase().startsWith("#SEP=")) {
                    sep = mobTrcLine.substring(5);
                    LOG.log(Level.INFO, "Metadata file for the mobility trace \"{0}\" uses separator=\"{1}\"",
                            new Object[]{metaDataPath, mobTrcCSVSep});
                }

                if (lineCSV.startsWith("#")) {
                    continue;
                }

                break;
            }

            LOG.log(Level.INFO,
                    "Initializing MUs on the area:\n\t{0}/{1}", new Object[]{0, musNum});
            int count = 0;
            int printPer = (int) (musNum * percentage);
            printPer = printPer == 0 ? 1 : printPer; // otherwise causes arithmetic exception devide by zero in some cases

            do {

                String[] csv = lineCSV.split(sep);

                int nxtMuID = Integer.parseInt(csv[0]);
//ignore these, zero init velocities for all mobiles                double avgdxdt = Double.parseDouble(csv[4]);
//                double avgdydt = Double.parseDouble(csv[6]);

                createTraceMU(nxtGroup, conn2SCPolicy, cachingPolicies, nxtMuID, area, mobTransDecisions, musLst);

//<editor-fold defaultstate="collapsed" desc="report/log progress">
                if (++count % 100 == 0 || count % printPer == 0) {
                    LOG.log(Level.INFO, "\tMobiles prepared: {0} "
                            + "out of {1}, i.e: {2}%",
                            new Object[]{
                                count, musNum,
                                Math.round(10000.0 * count / musNum) / 100.0
                            });
                }
//</editor-fold>
            } while ((lineCSV = metaIn.readLine()) != null);//for every MU__CLASS in group
        } catch (IOException ex) {
            throw new CriticalFailureException(ex);
        }

        try {
            updateTraceMU();
        } catch (IOException | StatisticException ex) {
            throw new CriticalFailureException(ex);
        } catch (TraceEndedException ex) {
            throw new CriticalFailureException("Trace ended too early during intiallization of mobiles", ex);
        }
    }
    private static final Logger LOG = Logger.getLogger(TraceTaxiesSimulation.class.getName());

    /**
     * Makes a first full parse of the mobility trace file to discover meta
     * data. Unless it finds #NUM, in which case it does not parse the whole
     * file (saves memory
     *
     * @param percentage
     * @param area
     * @param nxtGroup
     * @param conn2SCPolicy
     * @param cachingPolicies
     * @param mobTransDecisions
     * @param musLst
     * @param scenario
     * @throws CriticalFailureException
     * @throws InconsistencyException
     * @throws NumberFormatException
     */
    private void initAndConnectMUs_2(
            double percentage, Area area, MobileGroup nxtGroup,
            List<String> conn2SCPolicy, Collection<AbstractCachingPolicy> cachingPolicies,
            String mobTransDecisions, List<TraceMU> musLst, Scenario scenario)
            throws CriticalFailureException, InconsistencyException, NumberFormatException {

        String mutracePath = scenario.stringProperty(Space.MU__TRACE, true);
        String lineCSV;
        String sep = " ";

        int musNum = 0;
        SortedSet<Integer> ids = new TreeSet<>();
        try (BufferedReader bin = new BufferedReader(new FileReader(mutracePath))) {

            while ((lineCSV = bin.readLine()) != null) {

                if (lineCSV.toUpperCase().startsWith("#SEP=")) {
                    sep = mobTrcLine.substring(5);
                    LOG.log(Level.INFO, "Mobility trace \"{0}\" uses separator=\"{1}\"",
                            new Object[]{mutracePath, mobTrcCSVSep});
                }

                if (lineCSV.startsWith("#")) {
                    continue;
                }

                String[] csv = lineCSV.split(sep);
                int nxtMuID = Integer.parseInt(csv[1]);

                if (ids.add(nxtMuID)) {
                    musNum++;
                }
            }
        } catch (IOException ex) {
//            LOG.log(Level.SEVERE, null, ex);
            throw new CriticalFailureException(ex);
        }

        LOG.log(Level.INFO,
                "Initializing MUs on the area:\n\t{0}/{1}", new Object[]{0, musNum});

        int count = 0;
        int printPer = (int) (musNum * percentage);
        printPer = printPer == 0 ? 1 : printPer; // otherwise causes arithmetic exception devide by zero in some cases

        for (int nxtMuID : ids) {

            createTraceMU(nxtGroup, conn2SCPolicy, cachingPolicies, nxtMuID, area, mobTransDecisions, musLst);

//<editor-fold defaultstate="collapsed" desc="report/log progress">
            if (++count % 100 == 0 || count % printPer == 0) {
                LOG.log(Level.INFO, "\tMobiles prepared: {0} "
                        + "out of {1}, i.e: {2}%",
                        new Object[]{
                            count, musNum,
                            Math.round(10000.0 * count / musNum) / 100.0
                        });
            }
//</editor-fold>
        }

        try {
            updateTraceMU();
        } catch (IOException | StatisticException ex) {
            throw new CriticalFailureException(ex);
        } catch (TraceEndedException ex) {
            throw new CriticalFailureException("Trace ended too early during intiallization of mobiles", ex);
        }

    }

    private void createTraceMU(MobileGroup nxtGroup, List<String> conn2SCPolicy,
            Collection<AbstractCachingPolicy> cachingPolicies, int nxtMuID,
            Area area, String mobTransDecisions, List<TraceMU> musLst) {
        TraceMUBuilder nxtMUBuilder = new TraceMUBuilder(
                this, nxtGroup, area.getRandPoint(),
                conn2SCPolicy, cachingPolicies, 0, 0
        );

        nxtMUBuilder.setId(nxtMuID);

        nxtMUBuilder.setArea(area);

        nxtMUBuilder.setTransitionDecisions(mobTransDecisions);

        TraceMU mu = nxtMUBuilder.build();

        musLst.add(mu);
        int id = mu.getID();

        _muByID.put(id, mu);
        mu.setDX(5.25);
        mu.setDY(5.25);// 5.25 is the sqrt of 27.6, which is the average walking speed 5km/h, only in 20 sec
        _muMovingByID.put(id, mu);
        _muAvgVelocity = 27.6;

    }

    @Override
    @SuppressWarnings("empty-statement")
    public void run() {

        try {

            while (!Thread.currentThread().isInterrupted()
                    && checkWarmupDoInitialization(getTrcLoader())) {
                clock.tick();

                try {
                    updateTraceMU();
                } catch (TraceEndedException tee) {
                    throw new NormalSimulationEndException(tee);
                }
            };

            /*
             * if warmup period has passed..
             */
            WHILE_THREAD_NOT_INTERUPTED:
            while (!Thread.currentThread().isInterrupted()) {
                clock.tick();

                try {
                    updateTraceMU();
                } catch (TraceEndedException tee) {
                    throw new NormalSimulationEndException(tee);
                }

//////////////////////////////////////////////////                
//yyy                runGoldenRatioSearchEMPCLC();
//////////////////////////////////////////////////
                if (stationaryRequestsUsed()) {/*
                     * Concume data and keep gain stats for stationary users
                     */
                    for (SmallCell nxtSC : smallCells()) {
                        StationaryUser nxtSU = nxtSC.getStationaryUsr();
                        nxtSC.updtLclDmdByStationary(false);
                        nxtSU.consumeDataTry(1);
                        nxtSU.tryCacheRecentFromBH();// try to cache whatever not already in the cache that you just downloaded.
                    }

                }

/////////////////////////////////////
                List<TraceMU> shuffldMUs = shuffledMUs();
                _haveExitedPrevCell.clear();
                _haveHandedOver.clear();
                getStatsHandle().resetHandoverscount();

                for (TraceMU nxtMU : shuffldMUs) {
                    if (_muImmobileByID.containsKey(nxtMU.getID())) {
                        // avoid expensive call to moveRelatively() if possible
                        if (nxtMU.isSoftUser()) {
                            nxtMU.consumeTryAllAtOnceFromSC();
                        } else {
                            nxtMU.consumeDataTry(1);// consume in one simulation time step
                        }
                        continue;
                    }
                    nxtMU.moveRelatively(false, false);
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
