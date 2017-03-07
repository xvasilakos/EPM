/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sim.run.stats;

import app.properties.StatsProperty;
import app.properties.valid.Values;
import caching.base.AbstractCachingPolicy;
import exceptions.InvalidOrUnsupportedException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import sim.space.cell.smallcell.SmallCell;
import statistics.StatisticException;
import statistics.handlers.AbstractPerformanceStat;
import statistics.handlers.ICompute0;
import statistics.handlers.ICompute3;
import statistics.handlers.ICompute4;
import statistics.handlers.IComputePercent;
import statistics.handlers.iterative.sc.cmpt4.ComputeAllPoliciesImpl;
import statistics.handlers.iterative.sc.cmpt5.UnonymousCompute5;
import statistics.handlers.iterative.sc.cmpt6.UnonymousCompute6;
import utilities.CommonFunctions;

/**
 * Internal organisation class. Groups sets of statistics handlers.
 */
final class HandlersUsed {

    /**
     * for updating statistics after iterating mobile users regardless if they
     * move or not
     */
    final Set<statistics.handlers.IComputePercent> _handlers4Iterative__mu__cmpt1;
    /**
     * for updating statistics after mobile users last move.
     *
     * E.g: Gains etc.. from package stats.gainstats.gains
     */
    final Set<statistics.handlers.AbstractPerformanceStat> _handlers4Performance;
    /**
     * for updating statistics after iterating over SCs
     */
    final Set<ICompute4> _handlers4Iterative__sc__cmpt4_no_policies;
    final Set<ComputeAllPoliciesImpl> _handlers4Iterative__sc__cmpt4;
    final Set<UnonymousCompute5> _handlers4Iterative__sc__cmpt5;
    final Set<UnonymousCompute6> _handlers4Iterative__sc__cmpt6;
    /**
     * choose randomly one SC and study its behavior in each _sim round
     */
    final Set<ICompute0> _handlers4Fixed_sc__cmpt0;
    final Set<ICompute0> _handlers4Fixed_sc__cmpt0__no_policy;
    List<SmallCell> _monitorSC; // the cell IDs to monitor specialy
    private final StatsHandling _statsHandlingOuter;
    private final Logger _logger;

    HandlersUsed(final StatsHandling outer) throws IllegalAccessException, ClassNotFoundException, RuntimeException, InstantiationException, InvalidOrUnsupportedException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException, StatisticException {
        _statsHandlingOuter = outer;
        _logger = CommonFunctions.getLoggerFor(StatsHandling.class, "simID=" + outer.simID());

        _monitorSC = monitorSCs(outer);
        _handlers4Fixed_sc__cmpt0 = initFixedSCCmpt0(StatsProperty.HANDLERS__FIXED_SC__CMPT0, _monitorSC);
        _handlers4Fixed_sc__cmpt0__no_policy = initFixedSCCmpt0NoPolicies(StatsProperty.HANDLERS__FIXED_SC__CMPT0__NO_POLICIES, _monitorSC);
        try {
            _handlers4Iterative__sc__cmpt4_no_policies = initCmpt4Nopolicies(StatsProperty.HANLDERS__ITERATIVE__SC__CMPT4__NO_POLICY);
            _handlers4Iterative__sc__cmpt4 = initCmpt4(StatsProperty.HANLDERS__ITERATIVE__SC__CMPT4);
            _handlers4Iterative__sc__cmpt5 = initCmpt5(StatsProperty.HANLDERS__ITERATIVE__SC__CMPT5);
            _handlers4Iterative__sc__cmpt6 = initCmpt6(StatsProperty.HANDLERS__ITERATIVE__SC__CMPT6);
        } catch (IllegalAccessException | ClassNotFoundException | RuntimeException | InstantiationException | InvalidOrUnsupportedException | NoSuchMethodException | InvocationTargetException | StatisticException ex) {
            throw new RuntimeException(ex);
        }
        try {
            _handlers4Iterative__mu__cmpt1 = initCmpt1(StatsProperty.HANDLERS__ITERATIVE__MU__CMPT1);
        } catch (IllegalAccessException | ClassNotFoundException | RuntimeException | InstantiationException | InvalidOrUnsupportedException ex) {
            throw new RuntimeException(ex);
        }
        try {
            _handlers4Performance = initPerformanceStats(StatsProperty.PERFORMANCE__GAINS);
            _handlers4Performance.addAll(initPerformanceStats(StatsProperty.PERFORMANCE__OTHER));
        } catch (IllegalAccessException | ClassNotFoundException | RuntimeException | InstantiationException | InvalidOrUnsupportedException | NoSuchMethodException | InvocationTargetException | StatisticException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Used to initialize stats handlers of type IComputePercent, for a given
     * StatsProperty. Note that the passed StatsProperty's name must coincide
     * with the package name of each statistics handler loaded.
     *
     */
    private Set<IComputePercent> initCmpt1(StatsProperty statsProp) throws IllegalAccessException, ClassNotFoundException, RuntimeException, InstantiationException, InvalidOrUnsupportedException {
        Set<IComputePercent> initHdlSet = new TreeSet<>();
        List<String> handleNames = _statsHandlingOuter._scenarioSetup.listOfStringsProperty(statsProp, false);
        for (String nxtHandlerName : handleNames) {
            switch (nxtHandlerName) {
                case Values.UNDEFINED:
                case Values.NONE:
                case Values.NULL:
                    continue;
                default:
                    Object newInstance = Class.forName(statsProp.propertyName() + "." + nxtHandlerName).newInstance();
                    if (!(newInstance instanceof IComputePercent)) {
                        throw new RuntimeException("No known stats handler:" + statsProp.name() + "." + nxtHandlerName);
                    }
                    IComputePercent hndlr = (IComputePercent) newInstance;
                    initHdlSet.add(hndlr);
                    _statsHandlingOuter._simStatististics.addTitle(hndlr.title());
                    _logger.log(Level.FINE, "Loaded new stats handler: {0}", newInstance.getClass().getCanonicalName());
            } //which
        } //for
        return initHdlSet;
    }

    /**
     * Used to initialize stats handlers of type AbstractPerformanceStat.
     *
     */
    private Set<AbstractPerformanceStat> initPerformanceStats(StatsProperty statsProp) throws IllegalAccessException, ClassNotFoundException, RuntimeException, InstantiationException, InvalidOrUnsupportedException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException, StatisticException {
        Set<AbstractPerformanceStat> initHdlSet = new TreeSet<>();
        List<String> handleNames = _statsHandlingOuter._scenarioSetup.listOfStringsProperty(statsProp, false);
        for (String nxtHandlerName : handleNames) {
            switch (nxtHandlerName) {
                case Values.UNDEFINED:
                case Values.NONE:
                case Values.NULL:
                    continue;
                default:
                    // for every caching policy used
                    for (AbstractCachingPolicy nxtCachingPolicy : _statsHandlingOuter._sim.getCachingPolicies()) {
                        Object iCompute2Object = Class.forName(statsProp.propertyName() + "." + nxtHandlerName).getConstructor(AbstractCachingPolicy.class).newInstance(nxtCachingPolicy);
                        if (!(iCompute2Object instanceof AbstractPerformanceStat)) {
                            throw new StatisticException("No known stats handler:" + statsProp.name() + "." + nxtHandlerName);
                        }
                        AbstractPerformanceStat hndlr = (AbstractPerformanceStat) iCompute2Object;
                        initHdlSet.add(hndlr);
                        _statsHandlingOuter._simStatististics.addTitle(hndlr.title("MU"));
                        _statsHandlingOuter._simStatististics.addTitle(hndlr.title("SU"));
                        _statsHandlingOuter._simStatististics.addTitle(hndlr.title());
                        _logger.log(Level.FINE, "Loaded new stats handler: {0}({1})", new String[]{iCompute2Object.getClass().getCanonicalName(), nxtCachingPolicy.toString()});
                    }
            } //which
        } //for
        return initHdlSet;
    }

    /**
     * Used to initialize stats handlers of type ICompute3, for a given
     * StatsProperty. Note that the passed StatsProperty's name must coincide
     * with the package name of each statistics handler loaded.
     *
     */
    private Set<ICompute3> initCmpt3(StatsProperty statsProp) throws IllegalAccessException, ClassNotFoundException, RuntimeException, InstantiationException, InvalidOrUnsupportedException, NoSuchMethodException, StatisticException, IllegalArgumentException, InvocationTargetException {
        Set<ICompute3> initHdlSet = new TreeSet<>();
        List<String> handleNames = _statsHandlingOuter._scenarioSetup.listOfStringsProperty(statsProp, false);
        for (String nxtHandlerArgs : handleNames) {
            switch (nxtHandlerArgs) {
                case Values.UNDEFINED:
                case Values.NONE:
                case Values.NULL:
                    continue;
                default:
                    /*
                     * a name and two arguments are expected such as:
                     * "DiffEval;caching.epc.Incremental;caching.epcIncrementalcpop.Cnc"
                     */
                    String statName;
                    String meth1;
                    String meth2;
                    StringTokenizer tok = new StringTokenizer(nxtHandlerArgs, "|");
                    try {
                        statName = tok.nextToken();
                        meth1 = tok.nextToken();
                        meth2 = tok.nextToken();
                    } catch (NoSuchElementException e) {
                        throw new StatisticException("Error during parsing: " + nxtHandlerArgs + ". A method name and two arguments in the form of " + "\"statName|arg1|arg2\" must be applied");
                    }
                    Object newInstance = Class.forName(statsProp.propertyName() + "." + statName).getConstructor(String.class, String.class).newInstance(meth1, meth2);
                    if (!(newInstance instanceof ICompute3)) {
                        throw new StatisticException("No known stats handler:" + statsProp.name() + "." + nxtHandlerArgs);
                    }
                    ICompute3 hndlr = (ICompute3) newInstance;
                    initHdlSet.add(hndlr);
                    _statsHandlingOuter._simStatististics.addTitle(hndlr.title());
                    _logger.log(Level.FINE, "Loaded new stats handler: {0}({1})", new String[]{newInstance.getClass().getCanonicalName(), hndlr.title()});
            } //which
        } //for
        return initHdlSet;
    }

    private Set<ICompute0> initFixedSCCmpt0(StatsProperty statsProp, Collection<SmallCell> monitorSCs) throws IllegalAccessException, ClassNotFoundException, RuntimeException, InstantiationException, InvalidOrUnsupportedException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException, StatisticException {
        Set<ICompute0> initHdlSet = new TreeSet<>();
        List<String> handleNames = _statsHandlingOuter._scenarioSetup.listOfStringsProperty(statsProp, false);
        for (String nxtHandlerName : handleNames) {
            switch (nxtHandlerName) {
                case Values.UNDEFINED:
                case Values.NONE:
                case Values.NULL:
                    continue;
                default:
                    for (SmallCell nxtMonitorSC : monitorSCs) {
                        for (AbstractCachingPolicy nxtCachePolicy : _statsHandlingOuter._sim.getCachingPolicies()) {
                            Object newInstance = Class.forName(statsProp.propertyName() + "." + nxtHandlerName).getConstructor(AbstractCachingPolicy.class, SmallCell.class).newInstance(nxtCachePolicy, nxtMonitorSC);
                            if (!(newInstance instanceof ICompute0)) {
                                throw new StatisticException("No known stats handler:" + statsProp.name() + "." + nxtHandlerName);
                            }
                            ICompute0 hndlr = (ICompute0) newInstance;
                            initHdlSet.add(hndlr);
                            _statsHandlingOuter._simStatististics.addTitle(hndlr.title());
                            _logger.log(Level.FINE, "Loaded new stats handler: {0}(monitorSC={1})", new String[]{newInstance.getClass().getCanonicalName(), String.valueOf(nxtMonitorSC)});
                        }
                    }
            } //which
        } //for
        return initHdlSet;
    }

    private Set<ICompute0> initFixedSCCmpt0NoPolicies(StatsProperty statsProp, Collection<SmallCell> monitorSCs) throws IllegalAccessException, ClassNotFoundException, RuntimeException, InstantiationException, InvalidOrUnsupportedException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException, StatisticException {
        Set<ICompute0> initHdlSet = new TreeSet<>();
        List<String> handleNames = _statsHandlingOuter._scenarioSetup.listOfStringsProperty(statsProp, false);
        for (String nxtHandlerName : handleNames) {
            switch (nxtHandlerName) {
                case Values.UNDEFINED:
                case Values.NONE:
                case Values.NULL:
                    continue;
                default:
                    for (SmallCell nxtMonitorSC : monitorSCs) {
                        Object newInstance = Class.forName(statsProp.propertyName() + "." + nxtHandlerName).getConstructor(SmallCell.class).newInstance(nxtMonitorSC);
                        if (!(newInstance instanceof ICompute0)) {
                            throw new StatisticException("No known stats handler:" + statsProp.name() + "." + nxtHandlerName);
                        }
                        ICompute0 hndlr = (ICompute0) newInstance;
                        initHdlSet.add(hndlr);
                        _statsHandlingOuter._simStatististics.addTitle(hndlr.title());
                        _logger.log(Level.FINE, "Loaded new stats handler: {0}(monitorSC={1})", new String[]{newInstance.getClass().getCanonicalName(), String.valueOf(nxtMonitorSC)});
                    }
            } //which
        } //for
        return initHdlSet;
    }

    private Set<ICompute4> initCmpt4Nopolicies(StatsProperty statsProp) throws IllegalAccessException, ClassNotFoundException, RuntimeException, InstantiationException, InvalidOrUnsupportedException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException, StatisticException {
        Set<ICompute4> initHdlSet = new TreeSet<>();
        List<String> handleNames = _statsHandlingOuter._scenarioSetup.listOfStringsProperty(statsProp, false);
        for (String nxtHandlerName : handleNames) {
            switch (nxtHandlerName) {
                case Values.UNDEFINED:
                case Values.NONE:
                case Values.NULL:
                    continue;
                default:
                    String forName = statsProp.propertyName() + "." + nxtHandlerName;
                    Object newInstance = Class.forName(forName).getConstructor().newInstance();
                    if (!(newInstance instanceof ICompute4)) {
                        throw new StatisticException("No known stats handler:" + forName);
                    }
                    ICompute4 hndlr = (ICompute4) newInstance;
                    initHdlSet.add(hndlr);
                    _statsHandlingOuter._simStatististics.addTitle(hndlr.title());
                    _logger.log(Level.FINE, "Loaded new stats handler: {0}", newInstance.getClass().getCanonicalName());
            } //which
        } //for
        return initHdlSet;
    }

    private Set<ComputeAllPoliciesImpl> initCmpt4(StatsProperty statsProp) throws IllegalAccessException, ClassNotFoundException, RuntimeException, InstantiationException, InvalidOrUnsupportedException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException, StatisticException {
        Set<ComputeAllPoliciesImpl> initHdlSet = new TreeSet<>();
        List<String> handleNames = _statsHandlingOuter._scenarioSetup.listOfStringsProperty(statsProp, false);
        for (String nxtHandlerName : handleNames) {
            switch (nxtHandlerName) {
                case Values.UNDEFINED:
                case Values.NONE:
                case Values.NULL:
                    continue;
                default:
                    for (AbstractCachingPolicy nxtPolicy : _statsHandlingOuter._sim.getCachingPolicies()) {
                        Object newInstance = Class.forName(statsProp.propertyName() + "." + nxtHandlerName).getConstructor(AbstractCachingPolicy.class).newInstance(nxtPolicy);
                        if (!(newInstance instanceof ComputeAllPoliciesImpl)) {
                            throw new StatisticException("No known stats handler:" + statsProp.name() + "." + nxtHandlerName);
                        }
                        ComputeAllPoliciesImpl hndlr = (ComputeAllPoliciesImpl) newInstance;
                        initHdlSet.add(hndlr);
                        _statsHandlingOuter._simStatististics.addTitle(hndlr.title());
                        _logger.log(Level.FINE, "Loaded new stats handler: {0}", newInstance.getClass().getCanonicalName());
                    }
            } //which
        } //for
        return initHdlSet;
    }

    private Set<UnonymousCompute5> initCmpt5(StatsProperty statsProp) throws IllegalAccessException, ClassNotFoundException, RuntimeException, InstantiationException, InvalidOrUnsupportedException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException, StatisticException {
        Set<UnonymousCompute5> initHdlSet = new TreeSet<>();
        List<String> handleNames = _statsHandlingOuter._scenarioSetup.listOfStringsProperty(statsProp, false);
        for (String nxtHandlerTitl : handleNames) {
            switch (nxtHandlerTitl) {
                case Values.UNDEFINED:
                case Values.NONE:
                case Values.NULL:
                    continue;
                default:
                    for (AbstractCachingPolicy nxtCachePolicy : _statsHandlingOuter._sim.getCachingPolicies()) {
                        UnonymousCompute5 hndlr = new UnonymousCompute5(nxtCachePolicy, nxtHandlerTitl);
                        initHdlSet.add(hndlr);
                        _statsHandlingOuter._simStatististics.addTitle(hndlr.title());
                        _logger.log(Level.FINE, "Loaded new stats handler: {0}", hndlr.title());
                    }
            } //which
        } //for
        return initHdlSet;
    }

    private Set<UnonymousCompute6> initCmpt6(StatsProperty statsProp) throws IllegalAccessException, ClassNotFoundException, RuntimeException, InstantiationException, InvalidOrUnsupportedException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException, StatisticException {
        Set<UnonymousCompute6> initHdlSet = new TreeSet<>();
        List<String> handleNames = _statsHandlingOuter._scenarioSetup.listOfStringsProperty(statsProp, false);
        for (String nxtHandlerTitl : handleNames) {
            switch (nxtHandlerTitl) {
                case Values.UNDEFINED:
                case Values.NONE:
                case Values.NULL:
                    continue;
                default:
                    UnonymousCompute6 hndlr6 = new UnonymousCompute6(nxtHandlerTitl);
                    initHdlSet.add(hndlr6);
                    _statsHandlingOuter._simStatististics.addTitle(hndlr6.title());
                    _logger.log(Level.INFO, "Loaded new stats handler: {0}", hndlr6.title());
            } //which
        } //for
        return initHdlSet;
    }

    List<SmallCell> monitorSCs(StatsHandling sh) {
        List<Integer> cellIDs = sh._sim.getScenario().listOfIntegersProperty(
                StatsProperty.HANDLERS__FIXED_SC__MONITOR_SCS);
        List<SmallCell> cells = new ArrayList();
        for (int i = 0; i < cellIDs.size(); i++) {
            Integer nxtID = cellIDs.get(i);
            cells.add(sh._sim.getCellRegistry().getCellByID(nxtID));
        }

        return cells;
    }

}
