/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sim.run.stats;

import sim.ISimulationMember;
import sim.Scenario;
import sim.run.SimulationBaseRunner;
import app.SimulatorApp;
import app.properties.StatsProperty;
import exceptions.CriticalFailureException;
import exceptions.InvalidOrUnsupportedException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import sim.content.request.DocumentRequest;
import sim.space.cell.CellRegistry;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;
import sim.space.users.mobile.MobileUser;
import statistics.StatisticException;
import statistics.Statistics;
import statistics.handlers.ICompute0;
import statistics.handlers.ICompute4;
import statistics.handlers.iterative.sc.cmpt4.ComputeAllPoliciesImpl;
import statistics.handlers.iterative.sc.cmpt5.UnonymousCompute5;
import statistics.handlers.iterative.sc.cmpt6.UnonymousCompute6;
import statistics.output.Printer;
import statistics.handlers.IComputePercent;
import statistics.handlers.AbstractPerformanceStat;
import statistics.handlers.iterative.sc.cmpt6.UnonymousCompute6.WellKnownTitle;
import utils.CommonFunctions;

/**
 * Methods for handling statistics for this _sim.
 */
public final class StatsHandling implements ISimulationMember {

    private final Logger _logger;

    private final Printer _printer4Aggregates;
    private final Printer _printer4TransientAggregates;
    private final Printer _printer4Transient;
    private final Queue<String> _resultsTransientQ;
    private final boolean _printStddev;
    private final int _clockMaxTime;
    private final int _minTime;
    private final Statistics.ConfidenceInterval confInterval;
    private final boolean _printMean;
    private final boolean _printAggregates;
    private final boolean _printTransient;
    private final int _printTransientFlushPeriod;
    private final int _aggregatesAvgPeriod;
    private int _statHandoversCount;
    private boolean _isPrintingLocked = false;
    private final HandlersUsed _handlersUsed;

    protected final SimulationBaseRunner<?> _sim;
    protected final Scenario _scenarioSetup;
    protected final Statistics _simStatististics;

    public StatsHandling(final SimulationBaseRunner<?> sim) {
        this._sim = sim;
        this._logger = CommonFunctions.getLoggerFor(StatsHandling.class);

        this._scenarioSetup = sim.getScenario();

        try {
            // Load from scenario parameters
            _printMean = _scenarioSetup.isTrue(StatsProperty.STATS__PRINT__MEAN);
            _printStddev = _scenarioSetup.isTrue(StatsProperty.STATS__PRINT__STDDEV);
            _printAggregates = _scenarioSetup.isTrue(StatsProperty.STATS__PRINT__AGGREGATES);
            _printTransient = _scenarioSetup.isTrue(StatsProperty.STATS__PRINT__TRANSIENT);
            _printTransientFlushPeriod = _scenarioSetup.intProperty(StatsProperty.STATS__PRINT__TRANSIENT__FLUSH_PERIOD);
            _aggregatesAvgPeriod = _scenarioSetup.intProperty(StatsProperty.STATS__AGGREGATES__AVG_PERIOD);
            if (_aggregatesAvgPeriod <= 0) {
                //
                throw new RuntimeException(StatsProperty.STATS__AGGREGATES__AVG_PERIOD.name() + " property must be a positive integer value." + " It currently set to " + _aggregatesAvgPeriod);
            }
            _clockMaxTime = _scenarioSetup.intProperty(app.properties.Simulation.Clock.MAX_TIME);
            if (_clockMaxTime <= 0) {
                //
                throw new RuntimeException("Parameter " + app.properties.Simulation.Clock.MAX_TIME.propertyName() + " does not accept negative interger or zero values: " + _clockMaxTime);
            }

            _minTime = _scenarioSetup.intProperty(StatsProperty.STATS__MIN_TIME);
            if (_minTime < 0) {
                //
                throw new RuntimeException(StatsProperty.STATS__MIN_TIME + "  property can not be a negative integer value.");
            }

            if (_minTime >= _clockMaxTime) {
                //
                throw new RuntimeException(StatsProperty.STATS__MIN_TIME.name() + " property = " + _minTime + " can not be set to less than property " + app.properties.Simulation.Clock.MAX_TIME.name() + " = " + _clockMaxTime);
            }

            String _confIntervalZ = _scenarioSetup.stringProperty(StatsProperty.STATS__CONF_INTERVAL_Z, false);
            confInterval = Statistics.ConfidenceInterval.find(_confIntervalZ);
            String currSimStatsDirPath = SimulatorApp.getStatsDirPath();
            this._simStatististics = new Statistics(sim);

            //<editor-fold defaultstate="collapsed" desc="initialize printers for aggregated and transient results">
            if (_printAggregates) {
                this._printer4Aggregates = new Printer(sim, _scenarioSetup, currSimStatsDirPath, "agg", ".csv");
            } else {
                this._printer4Aggregates = null;
            }
            if (_printTransient) {
                this._printer4Transient = new Printer(sim, _scenarioSetup, currSimStatsDirPath, "trn", ".csv");
                this._printer4TransientAggregates = new Printer(sim, _scenarioSetup, currSimStatsDirPath, "trnAgg", ".csv");
                _resultsTransientQ = new LinkedList<>();
            } else {
                this._printer4Transient = null;
                this._printer4TransientAggregates = null;
                this._resultsTransientQ = null;
            }
            //</editor-fold>

            try {
                _handlersUsed = new HandlersUsed(this);
            } catch (IllegalAccessException | ClassNotFoundException |
                    InstantiationException | NoSuchMethodException |
                    IllegalArgumentException | InvocationTargetException ex) {
                throw new StatisticException("Can not initialize handlers for statistics", ex);
            }

        } catch (InvalidOrUnsupportedException | RuntimeException |
                StatisticException | IOException ex) {
            throw new CriticalFailureException(ex);
        }

    }

    public boolean isStatsMinTimeExceeded() {
        return _sim.simTime() > _minTime;
    }

    public void updtFixedSC() throws StatisticException {

        int recordingTime = statRecordingAvgPeriodTime();
        if (!_handlersUsed._handlers4Fixed_sc__cmpt0.isEmpty()) {
            for (ICompute0 handler : _handlersUsed._handlers4Fixed_sc__cmpt0) {
                double val = handler.compute0();
                if (val == -1) {
                    continue;
                }
                _simStatististics.addValuesForTime(recordingTime, handler.title(), val);
            }
        } //</editor-fold>
        if (!_handlersUsed._handlers4Fixed_sc__cmpt0__no_policy.isEmpty()) {
            for (ICompute0 handler : _handlersUsed._handlers4Fixed_sc__cmpt0__no_policy) {
                double val = handler.compute0();
                if (val == -1) {
                    continue;
                }
                _simStatististics.addValuesForTime(recordingTime, handler.title(), val);
            }
        } //</editor-fold>
    }

    /**
     * must be called after moving and updating connectivity status
     *
     *
     * @param mu
     * @throws Exception
     */
    public void updtIterativeMU(MobileUser mu) throws Exception {
        if (!isStatsMinTimeExceeded() || _handlersUsed._handlers4Iterative__mu__cmpt1.isEmpty()) {
            return;
        }
        int recordingTime = statRecordingAvgPeriodTime();
        Iterator<IComputePercent> stat_iter = _handlersUsed._handlers4Iterative__mu__cmpt1.iterator();
        while (stat_iter.hasNext()) {
            IComputePercent nxtHandler = stat_iter.next();
            _simStatististics.addValuesForTime(recordingTime,
                    nxtHandler.title(),
                    nxtHandler.computePercent(
                            _sim, mu, mu.getCurrentlyConnectedSC()
                    )
            );
        }
    }

    /**
     * @param cu
     * @throws exceptions.InvalidOrUnsupportedException
     * @throws statistics.StatisticException Caution call before updating cache
     * decisions
     */
    public void updtPerformanceStats(CachingUser cu) throws InvalidOrUnsupportedException, StatisticException {
        if (!isStatsMinTimeExceeded()) {
            return;
        }
        int recordingTime = statRecordingAvgPeriodTime();

        if (!_handlersUsed._handlers4Performance.isEmpty()) {
            Iterator<AbstractPerformanceStat> statIter = _handlersUsed._handlers4Performance.iterator();
            for (DocumentRequest nxtRequest : cu.getRequests()) {

                // if 
                // 1. not fully consumed and 
                // 2. modile user is hard (i.e. it keeps the request after 
                //    disconnecting and keeps consuming), 
                // then hits will be re-considered each time the mobile exits a cell
                if (cu instanceof MobileUser && !nxtRequest.isFullyConsumed()) {
                    if (((MobileUser) cu).isHardUser()) {
                        continue;// let, e.g., the stationary log their gains 
                        // when they update their requests               
                    }
                }

                while (statIter.hasNext()) {
                    AbstractPerformanceStat nxtStat = statIter.next();
                    double value = nxtStat.computeGain(cu, nxtRequest);

                    if (Double.isNaN(value)) {
                        continue;//ignore
                    }

                    // here for the refined category per user type
                    _simStatististics.addValuesForTime(recordingTime,
                            nxtStat.title((cu instanceof MobileUser ? "MU" : "SU")),
                            value);

                    // here for the general category of all user types
                    _simStatististics.addValuesForTime(recordingTime,
                            nxtStat.title(),
                            value);

                }
            }
        }
    }

    /**
     * Examples: BuffUsed, Hit%, MUsCurrConnected
     *
     * @throws StatisticException
     */
    public void updtIterativeSCCmpt4() throws StatisticException {

        int recordingTime = statRecordingAvgPeriodTime();
        if (!_handlersUsed._handlers4Iterative__sc__cmpt4.isEmpty()) {
            Iterator<SmallCell> scIterator = _sim.getCellRegistry().getSmallCells().iterator();
            while (scIterator.hasNext()) {
                SmallCell nxtSC = scIterator.next();
                for (ComputeAllPoliciesImpl nxtStatHandler : _handlersUsed._handlers4Iterative__sc__cmpt4) {
                    ComputeAllPoliciesImpl handler = nxtStatHandler;
                    double val = handler.compute4(nxtSC);
                    if (val == -1) {
                        continue;
                    }
                    _simStatististics.addValuesForTime(recordingTime, handler.title(), val);
                }
            }
        } //</editor-fold>
    }

    /**
     * Same as in updtIterativeSCCmpt4, only without respect to cache policies.
     * Examples: NeighborsCount
     *
     * @throws StatisticException
     */
    public void updtIterativeSCCmpt4NoCachePolicy() throws StatisticException {

        int recordingTime = statRecordingAvgPeriodTime();
        if (!_handlersUsed._handlers4Iterative__sc__cmpt4_no_policies.isEmpty()) {
            Iterator<SmallCell> scIterator = _sim.getCellRegistry().getSmallCells().iterator();
            while (scIterator.hasNext()) {
                SmallCell nxtSC = scIterator.next();
                for (ICompute4<SmallCell> nxtStatHandler : _handlersUsed._handlers4Iterative__sc__cmpt4_no_policies) {
                    ICompute4<SmallCell> handler = nxtStatHandler;
                    double val = handler.compute4(nxtSC);
                    if (val == -1) {
                        continue;
                    }
                    _simStatististics.addValuesForTime(recordingTime, handler.title(), val);
                }
            }
        } //</editor-fold>
    }

    /**
     * Regards UnonymousCompute5 stats.
     *
     * @param d
     * @param stat
     * @throws statistics.StatisticException
     */
    public void updtSCCmpt5(double d, UnonymousCompute5 stat) throws StatisticException {
        if (!isStatsMinTimeExceeded()) {
            return;
        }
        int recordingTime = statRecordingAvgPeriodTime();
        if (_handlersUsed._handlers4Iterative__sc__cmpt5.contains(stat)) {
            _simStatististics.addValuesForTime(recordingTime, stat.title(), stat.compute5_6(d));
        }
    }

    /**
     * Regards UnonymousCompute6 stats.
     *
     * @param d
     * @param stat
     */
    public void updtSCCmpt6(double d, UnonymousCompute6 stat) {
        if (!isStatsMinTimeExceeded()) {
            return;
        }
        int recordingTime = statRecordingAvgPeriodTime();
        try {
            _simStatististics.addValuesForTime(recordingTime, stat.title(), stat.compute5_6(d));
        } catch (StatisticException ex) {
            _logger.log(Level.SEVERE, null, ex);
        }
    }

    public void updtSCCmpt6(double d, String stat) {
        WellKnownTitle wkstat = new UnonymousCompute6.WellKnownTitle(stat);
        UnonymousCompute6 ustat = new UnonymousCompute6(wkstat);
        updtSCCmpt6(1, ustat);
    }

    public Statistics getStats() {
        return this._simStatististics;
    }

    /**
     * Averaging statistics period that the current simulation time belongs to.
     * Averaging periods (e.g per 20 time units) are used for
     * compressing/aggregating result outputs per, e.g. 20 time units. As a side
     * effect, the bigger the averaging period, the more smooth the resulting
     * curves in the corresponding plots are.
     *
     * @return
     */
    public int statRecordingAvgPeriodTime() {
        // e.g. 20 * ((int) 18 / 20) =20 * ((int) 0.9) = 0
        // e.g. 20 * (21 / 20) = 20 * ((int)1.05) = 20 * 1 = 20
        return _aggregatesAvgPeriod * (_sim.simTime() / _aggregatesAvgPeriod);
    }

    /**
     * Committing implies finalizing and thus compressing state in each record.
     *
     * This method must be called after recording all statistics for a give _sim
     * simTime, to commit all statistics recorded for this _sim simTime.
     *
     * @return true iff stats are committed
     * @throws statistics.StatisticException
     *
     * note: Commit takes actually place if and only if (i) the current time
     * denotes that previously recorded statistics were the last ones in a
     * series of statistics of the averaging/smoothing period, and (ii) the
     * minimum actual simulation time for keeping statistics is exceeded.
     */
    public boolean tryCommitRound() throws StatisticException {
        int statRecordingAvgPeriodTime = statRecordingAvgPeriodTime();
        /*Caution must use this averaging period simTime instead of actual _sim clock simTime;
             * otherwise it tries to finalize times that may have not been recorded*/
        if (_aggregatesAvgPeriod == 1 || _sim.simTime() + 1 == statRecordingAvgPeriodTime) {
            // finalize if this is the last recorded series of _simStatististics
            _simStatististics.finalizeState(statRecordingAvgPeriodTime);
            return true;
        }
        return false;
    }

    /**
     * Spawns a thread that prints aggregated.
     *
     * @throws StatisticException
     */
    public void prntAggregates() throws StatisticException {
        if (!isStatsMinTimeExceeded()) {
            return;
        }
        if (!_printAggregates) {
            _logger.warning(
                    "Output method for aggregated results invoked but"
                    + " ignored. Verify stats printing "
                    + "properties value.");
            return;
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                String aggregatedValues;
                try {
                    synchronized (_simStatististics) {
                        while (_isPrintingLocked && !Thread.currentThread().isInterrupted()) {
                            try {
                                _simStatististics.wait();
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
                        }
                        _isPrintingLocked = true;
                        aggregatedValues = _simStatististics.resultsAggregated(_printMean,
                                _printStddev,
                                confInterval,
                                (int) Math.max(_minTime, 0.75 * simTime())/*count ony the last 25% of results*/,
                                simTime(),
                                true, false);
                        _isPrintingLocked = false;
                        _simStatististics.notifyAll();
                    }
                } catch (StatisticException | InvalidOrUnsupportedException ex) {
                    _logger.log(Level.SEVERE, null, ex);
                    throw new RuntimeException(ex);
                }
                _printer4Aggregates.print(aggregatedValues);
                _printer4Aggregates.close();
            }
        };
        new Thread(runnable).start();
    }

    /**
     * Spawns a thread that prints results in incremental simTime sequence.
     *
     * @param flushClose close the stream after invocation.
     * @throws StatisticException
     */
    public void appendTransient(final boolean flushClose) throws StatisticException {
        if (!isStatsMinTimeExceeded()) {
            return;
        }

        if (!_printTransient) {
            return;
        }

        try {
            String transientValues = _simStatististics.
                    resultsTransient(
                            false, _printMean, _printStddev,
                            confInterval
                    );

            //<editor-fold defaultstate="collapsed" desc="block if printing results">
            synchronized (_simStatististics) {
                while (_isPrintingLocked && !Thread.currentThread().isInterrupted()) {
                    try {
                        _simStatististics.wait();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }

                _isPrintingLocked = false;
                _simStatististics.notifyAll();
            }
            //</editor-fold>

            _resultsTransientQ.add(transientValues);

            String aggregatedValues;
            try {
                aggregatedValues = _simStatististics.resultsAggregated(_printMean,
                        _printStddev,
                        confInterval,
                        (int) Math.max(_minTime, 0.75 * simTime())/*count ony the last 25% of results*/,
                        simTime(), false, true);
            } catch (StatisticException | InvalidOrUnsupportedException ex) {
                _logger.log(Level.SEVERE, null, ex);
                throw new RuntimeException(ex);
            }

            if (!flushClose) {
                // so that it does not double print the results at the end of the transAggregated csv file.
                _printer4TransientAggregates.print(aggregatedValues);
            } else {
                checkFlushTransient(true);
                _printer4Transient.close();
                _printer4TransientAggregates.close();
            }
        } catch (StatisticException ex) {
            _logger.log(Level.SEVERE, null, ex);
            throw new CriticalFailureException(ex);
        }

    }

    public void checkFlushTransient(boolean force) {
        if (simTime() % _printTransientFlushPeriod == 0 || force) {
            Runnable runPrint = new Runnable() {
                @Override
                public void run() {
                    synchronized (_simStatististics) {
                        while (_isPrintingLocked && !Thread.currentThread().isInterrupted()) {
                            try {
                                _simStatististics.wait();
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
                        }
                        _isPrintingLocked = true;
                        for (String nxtResults : _resultsTransientQ) {
                            _printer4Transient.print(nxtResults);
                        }
                        _resultsTransientQ.clear();

                        _isPrintingLocked = false;
                        _simStatististics.notifyAll();
                    }

                }
            };

            new Thread(runPrint).start();
        }
    }

    public void incHandoverscount() {
        _statHandoversCount++;
    }

    public void resetHandoverscount() {
        _statHandoversCount = 0;
    }

    public void statHandoversCount() throws StatisticException {
        updtSCCmpt6(_statHandoversCount, new UnonymousCompute6("n_Handovers"));
    }

    @Override
    public int simTime() {
        return _sim.simTime();
    }

    @Override
    public String simTimeStr() {
        return "[" + simTime() + "]";
    }

    @Override
    public int simID() {
        return _sim.simID();
    }

    @Override
    public SimulationBaseRunner getSim() {
        return _sim;
    }

    @Override
    public CellRegistry simCellRegistry() {
        return _sim.simCellRegistry();
    }

}
