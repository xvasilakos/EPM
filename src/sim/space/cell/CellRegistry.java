package sim.space.cell;

import app.properties.Simulation;
import app.properties.Space;
import app.properties.valid.Values;
import caching.base.AbstractCachingPolicy;
import exceptions.CriticalFailureException;
import exceptions.InconsistencyException;
import exceptions.InvalidOrUnsupportedException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.logging.Level;
import sim.ISimulationMember;
import utils.ISynopsisString;
import java.util.logging.Logger;
import sim.Scenario;
import sim.run.SimulationBaseRunner;
import sim.space.Area;
import sim.space.Point;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.UserGroup;
import sim.space.users.mobile.MobileGroup;
import sim.space.users.mobile.MobileGroupsRegistry;
import sim.space.users.mobile.MobileUser;
import utils.CommonFunctions;
import utilities.Couple;

/**
 * A registry of macro cell and small cells
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class CellRegistry implements ISimulationMember, ISynopsisString {

    private final SimulationBaseRunner _sim;
    private final Scenario _s;
    private final Logger _logger;

    /**
     * key: ID of SC
     *
     * Value: SmallCell with the given ID
     */
    private final Map<Integer, SmallCell> _smallCells;
    private final MacroCell _macroCell;
    /**
     * Number of Handoffs from a source smaller, mapped to a key that depends on
     * probabilities policy.
     *
     * Uses a string key which depends on the handoff probabilities computation
     * policy. If the key is the id of a small cell only, then probabilities
     * depend only on the location of MUs before they handoff. Alternatively, if
     * the key depends on both the MUs' group and their cell, then probabilities
     * are computed per location and group.
     */
    private final Map<String, Double> _handoffsOutgoing = new HashMap<>(20, 2);
    private final Map<String, Double> _handoffsIncoming = new HashMap<>(20, 2);
    private final Map<Couple<String, String>, Double> _handoffsBetween = new HashMap<>(20, 2);
    /**
     * Handover duration from a coming cell (couple.first is the disconnection
     * cell) to a destination cell (couple.second is the connection cell)
     */
    private final Map<Couple<String, String>, Double> interCellHandoverDuration = new HashMap<>(20, 2);
    /**
     * The last thirty samples used for computing the stdev
     */
    private final Map<Couple<String, String>, Couple<double[], Integer>> interCellHandoverDurationLastSamples = new HashMap<>(20, 2);
    private static final int SAMPLES_SIZE = 30;
    

    /**
     * Residence duration when coming from another cell (couple.first is the
     * former cell and couple.second is the residence cell)
     */
    private final Map<Couple<String, String>, Double> interCellResidenceDuration = new HashMap<>(20, 2);
    private final Map<Couple<String, String>, Couple<double[], Integer>> interCellResidenceDurationLastSamples = new HashMap<>(20, 2);
    private final String _mobilityModel;
    private final MobileGroupsRegistry _muGroupRegistry;
    private final double _probJitter;

    public CellRegistry(
            SimulationBaseRunner sim, MobileGroupsRegistry groupRegistry,
            MacroCell mc, Area area) throws InvalidOrUnsupportedException {

        _sim = sim;
        _s = _sim.getScenario();
        _logger = CommonFunctions.getLoggerFor(CellRegistry.class, "simID=" + getSimulation().getID());

        _mobilityModel = _s.stringProperty(Space.MOBILITY_MODEL, false);
        _probJitter = _s.doubleProperty(Space.SC__HANDOFF_PROBABILITY__STDEV);

        this._muGroupRegistry = groupRegistry;

        this._smallCells = new TreeMap<>();
        Collection<SmallCell> scs = createSCs(area, sim.getCachingStrategies());
        Iterator<SmallCell> cellsIter = scs.iterator();
        while (cellsIter.hasNext()) {
            SmallCell sc = cellsIter.next();
            int scID = sc.getID();
            this._smallCells.put(scID, sc);
        }

        this._macroCell = mc;

    }

    @Override
    public String toString() {
        return this._sim.toString() + " "
                + this._muGroupRegistry.toString() + " "
                + "; Cells: <" + CommonFunctions.toString(_smallCells)
                + ">; macrocell: " + _macroCell.toString()
                + "; mobility model: " + _mobilityModel
                + "; probability jitter: " + _probJitter;
    }

    public String toSynopsisString() {
        return this._sim.toString() + " "
                + this._muGroupRegistry.toSynopsisString() + " "
                + "; Cells: <" + CommonFunctions.toString(_smallCells)
                + ">";
    }

    public void printProbsToCurrentPath() {
        try (PrintStream printer = new PrintStream("./probs.txt")) {
            Set<Couple<String, String>> betweenCellsKeySet = _handoffsBetween.keySet();
            Iterator<Couple<String, String>> betweenCells_it = betweenCellsKeySet.iterator();
            while (betweenCells_it.hasNext()) {
                Couple<String, String> couple = betweenCells_it.next();
                printer.println("src: " + couple.getFirst());
                printer.println("dest: " + couple.getSecond());
            }
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @return the cellRegistry
     */
    public Collection<SmallCell> getSmallCells() {
        return Collections.unmodifiableCollection(_smallCells.values());
    }

    /**
     * @return the macroCell
     */
    public MacroCell getMacroCell() {
        return _macroCell;
    }

    /**
     * @return a randomly chosen small cell from the registry
     */
    public SmallCell rndSC() {
        Collection<SmallCell> scs = getSmallCells();

        int size = scs.size();
        int rnd = _sim.getRandomGenerator().randIntInRange(0, size - 1);
        int i = 0;
        for (SmallCell cell : scs) {
            if (i == rnd) {
                return cell;
            }
            i++;
        }

        throw new RuntimeException("Wrong random number generated.");
    }

    /**
     * mu
     *
     * @return the small cell covering the current position of mu that is closer
     * to the mu, or null if the mu is out of coverage of any small cell.
     */
    public PriorityQueue<SmallCell> closestCoveringSCs(MobileUser mu) {
        Point muPoint = mu.getCoordinates();
        return muPoint.getClosestCoveringSCs(false, true);
    }

    public SmallCell coveringRandomSmallcell(MobileUser mu) throws InvalidOrUnsupportedException {
        Point muPoint = mu.getCoordinates();
        List<SmallCell> coveringSmallerCells = new ArrayList(muPoint.getCoveringSCs());
        if (coveringSmallerCells.isEmpty()) {
            return null;
        }

        int lastPos = coveringSmallerCells.size() - 1;
        int rndPos = _sim.getRandomGenerator().randIntInRange(0, lastPos);
        return coveringSmallerCells.get(rndPos);
    }

    @Override
    public final int simID() {
        return _sim.getID();
    }

    @Override
    public final SimulationBaseRunner getSimulation() {
        return _sim;
    }

    @Override
    public final int simTime() {
        return _sim.simTime();
    }

    @Override
    public String simTimeStr() {
        return "[" + simTime() + "]";
    }

    @Override
    public CellRegistry simCellRegistry() {
        return this;
    }

    /**
     * Update how much time has a mobile stayed connected to nextSC given that
     * it was handed over to nextSC from prevSC.
     *
     * grp comingFrom residenceSC newDuration
     */
    public void updtResidenceTime(UserGroup grp, SmallCell comingFrom,
            SmallCell residenceSC, int newDuration) {

        if (comingFrom == null) {
            return; // can happen at startup
        }

        String comingFromID, residenceID;
        switch (_mobilityModel) {
            case Values.LOCATION:
                comingFromID = comingFrom.getID() + "";
                residenceID = residenceSC.getID() + "";
                break;

            case Values.LOCATION__PLUS__GROUP:
                comingFromID = CommonFunctions.combineCellMUGroup(comingFrom, grp == null ? - 1 : grp.getId());
                residenceID = CommonFunctions.combineCellMUGroup(residenceSC, grp == null ? - 1 : grp.getId());
                break;

            default:
                throw new UnsupportedOperationException("Unknonwn or unsupported policy " + _mobilityModel
                        + " set for parameter " + Space.MOBILITY_MODEL.propertyName());
        }//switch

        Double historyDuration = interCellResidenceDuration.get(new Couple(comingFromID, residenceID));
        Couple theCells = new Couple(comingFromID, residenceID);

        double newWeight = residenceSC.getConnectedMUs().isEmpty() ? 0.3 : 1.0 / (residenceSC.getConnectedMUs().size());
        if (historyDuration == null) {
            historyDuration = getSimulation().getScenario().doubleProperty(Space.SC__INIT_DURATION__RESIDENCE);

            double samples[] = new double[SAMPLES_SIZE];
            interCellResidenceDurationLastSamples.put(theCells, new Couple(samples, 0));
        } else {
            // cyclic update of last #SAMPLES_SIZE samples
            Couple<double[], Integer> samples2Idx = interCellResidenceDurationLastSamples.get(theCells);
            int idx = (1 + samples2Idx.getSecond()) % SAMPLES_SIZE;
            samples2Idx.getFirst()[idx] = newDuration;
            samples2Idx.setSecond(idx);
        }

        residenceSC.updtSmoothedResidenceDuration(newDuration, newWeight);

        double val = newWeight * newDuration + (1 - newWeight) * historyDuration;

        interCellResidenceDuration.put(theCells, val);
    }

    public void updtHandoverTransitionTime(MobileUser mu, SmallCell disconFrom, SmallCell conTo, int newDuration) {

        UserGroup grp = mu.getUserGroup();

        if (disconFrom == null) {
            return; // can happen at startup
        }

        String disconFromID, conToID;
        switch (_mobilityModel) {
            case Values.LOCATION:
                disconFromID = disconFrom.getID() + "";
                conToID = conTo.getID() + "";
                break;

            case Values.LOCATION__PLUS__GROUP:
                disconFromID = CommonFunctions.combineCellMUGroup(disconFrom, grp == null ? - 1 : grp.getId());
                conToID = CommonFunctions.combineCellMUGroup(conTo, grp == null ? - 1 : grp.getId());
                break;

            default:
                throw new UnsupportedOperationException("Unknonwn or unsupported policy " + _mobilityModel
                        + " set for parameter " + Space.MOBILITY_MODEL.propertyName());
        }//switch

        Couple theCells = new Couple(disconFromID, conToID);
        Double historyDuration = interCellHandoverDuration.get(theCells);
        if (historyDuration == null) {
            historyDuration = getSimulation().getScenario().doubleProperty(Space.SC__INIT_DURATION__HANDOVER);

            double samples[] = new double[SAMPLES_SIZE];
            interCellHandoverDurationLastSamples.put(theCells, new Couple(samples, 0));
        } else {
            // cyclic update of last #SAMPLES_SIZE samples
            Couple<double[], Integer> samples2Idx = interCellHandoverDurationLastSamples.get(theCells);
            int idx = (1 + samples2Idx.getSecond()) % SAMPLES_SIZE;
            samples2Idx.getFirst()[idx] = newDuration;
            samples2Idx.setSecond(idx);
        }

        double newWeight = 0.3;

        conTo.updtAvgHandoverDuration(newDuration, newWeight);

        double val = newWeight * newDuration + (1 - newWeight) * historyDuration;

        interCellHandoverDuration.put(theCells, val);
    }

    /**
     * Updates the history of handoff probabilities between handoff source and
     * handoff destination SCs.
     *
     * mu src dest
     */
    public void updtHandoffProbs(MobileUser mu, SmallCell src, SmallCell dest) {

        UserGroup grp = mu.getUserGroup();

        String srcID, destID;
        // increase transtion-counting maps //
        switch (_mobilityModel) {
            case Values.LOCATION:
                srcID = src.getID() + "";
                destID = dest.getID() + "";
                break;

            case Values.LOCATION__PLUS__GROUP:
                srcID = CommonFunctions.combineCellMUGroup(src, grp == null ? - 1 : grp.getId());
                destID = CommonFunctions.combineCellMUGroup(dest, grp == null ? - 1 : grp.getId());
                break;

            default:
                throw new UnsupportedOperationException("Unknonwn or unsupported policy " + _mobilityModel
                        + " set for parameter " + Space.MOBILITY_MODEL.propertyName());
        }//switch
        Couple couple = new Couple(srcID, destID);
        Double outgoing = _handoffsOutgoing.get(srcID);
        Double incoming = _handoffsIncoming.get(destID);
        Double between = _handoffsBetween.get(couple);
        _handoffsOutgoing.put(srcID, outgoing == null ? 1 : outgoing + 1);
        _handoffsIncoming.put(destID, incoming == null ? 1 : incoming + 1);
        _handoffsBetween.put(couple, between == null ? 1 : between + 1);
    }

    /**
     * grp src dest
     *
     * @return the handoffs__total__Src_toDestCell
     */
    public double getHandoffsBetweenCells(UserGroup grp, SmallCell src, SmallCell dest) {

        String srcID, destID;
        // increase transtion-counting maps //
        switch (_mobilityModel) {

            case Values.LOCATION:
                srcID = src.getID() + "";
                destID = dest.getID() + "";
                break;

            case Values.LOCATION__PLUS__GROUP:
                srcID = CommonFunctions.combineCellMUGroup(src, grp == null ? - 1 : grp.getId());
                destID = CommonFunctions.combineCellMUGroup(dest, grp == null ? - 1 : grp.getId());
                break;

            default:
                throw new UnsupportedOperationException("Unknonwn or unsupported policy " + _mobilityModel
                        + " for parameter " + Space.MOBILITY_MODEL.propertyName());
        }//switch
        Double num = _handoffsBetween.get(new Couple(srcID, destID));
        if (num == null) {
            return 0;
        }
        return num;
    }

    public Couple<Double, Double> getResidenceDurationBetween(UserGroup grp, SmallCell fromSC, SmallCell residentSC, boolean use95percentile) {
        String comingFromSCID, residentSCID;
        // increase transtion-counting maps //
        switch (_mobilityModel) {

            case Values.LOCATION:
                comingFromSCID = fromSC.getID() + "";
                residentSCID = residentSC.getID() + "";
                break;

            case Values.LOCATION__PLUS__GROUP:
                comingFromSCID = CommonFunctions.combineCellMUGroup(fromSC, grp == null ? - 1 : grp.getId());
                residentSCID = CommonFunctions.combineCellMUGroup(residentSC, grp == null ? - 1 : grp.getId());
                break;

            default:
                throw new UnsupportedOperationException("Unknonwn or unsupported policy " + _mobilityModel
                        + " for parameter " + Space.MOBILITY_MODEL.propertyName());
        }//switch

        Couple theCells = new Couple(comingFromSCID, residentSCID);

        Double avg = interCellResidenceDuration.get(theCells);

        if (avg == null) {
            return new Couple(100.0, 0.0);
        }

        double percentile95 = 0;
        if (use95percentile) {
            // compute stdev first
            double s = 0;
            double[] samples = interCellResidenceDurationLastSamples.get(theCells).getFirst();
            for (double nxtSample : samples) {
                s += Math.pow(avg - nxtSample, 2);
            }
            s = Math.sqrt(s);

            percentile95 = 1.96 * s;
        }

        return new Couple(avg, percentile95);
    }

    public Couple<Double, Double> getHandoverDurationBetween(UserGroup grp, SmallCell disconSC, SmallCell conToSC, boolean use95percentile) {

        String disconSCID, connSCID;
        // increase transtion-counting maps //
        switch (_mobilityModel) {

            case Values.LOCATION:
                disconSCID = disconSC.getID() + "";
                connSCID = conToSC.getID() + "";
                break;

            case Values.LOCATION__PLUS__GROUP:
                disconSCID = CommonFunctions.combineCellMUGroup(disconSC, grp == null ? - 1 : grp.getId());
                connSCID = CommonFunctions.combineCellMUGroup(conToSC, grp == null ? - 1 : grp.getId());
                break;

            default:
                throw new UnsupportedOperationException("Unknonwn or unsupported policy " + _mobilityModel
                        + " for parameter " + Space.MOBILITY_MODEL.propertyName());
        }//switch

        Couple theCells = new Couple(disconSCID, connSCID);
        Double avg = interCellHandoverDuration.get(theCells);

        if (avg == null) {
            return new Couple(100.0, 0.0);
        }

        double percentile95 = 0;
        if (use95percentile) {
            // compute stdev first
            double s = 0;
            double[] samples = interCellHandoverDurationLastSamples.get(theCells).getFirst();
            for (double nxtSample : samples) {
                s += Math.pow(avg - nxtSample, 2);
            }
            s = Math.sqrt(s);

            percentile95 = 1.96 * s;
        }

        return new Couple(avg, percentile95);
    }

    public double getHandoffsOutgoing(UserGroup grp, SmallCell src) {

        String srcID;
        // increase transtion-counting maps //
        switch (_mobilityModel) {

            case Values.LOCATION:
                srcID = src.getID() + "";
                break;

            case Values.LOCATION__PLUS__GROUP:
                srcID = CommonFunctions.combineCellMUGroup(src, grp == null ? - 1 : grp.getId());
                break;

            default:
                throw new UnsupportedOperationException("Unknonwn or unsupported policy " + _mobilityModel
                        + " for parameter " + Space.MOBILITY_MODEL.propertyName());
        }//switch
        Double num = _handoffsOutgoing.get(srcID);
        if (num == null) {
            return 0;
        }
        return num;
    }

    public double getHandoffsIncoming(MobileUser mu, SmallCell dest) {

        UserGroup grp = mu.getUserGroup();
        String destID;
        // increase transtion-counting maps //
        switch (_mobilityModel) {

            case Values.LOCATION:
                destID = dest.getID() + "";
                break;

            case Values.LOCATION__PLUS__GROUP:
                destID = CommonFunctions.combineCellMUGroup(dest, grp == null ? - 1 : grp.getId());
                break;

            default:
                throw new UnsupportedOperationException("Unknonwn or unsupported policy " + _mobilityModel
                        + " for parameter " + Space.MOBILITY_MODEL.propertyName());
        }//switch
        Double num = _handoffsIncoming.get(destID);
        if (num == null) {
            return 0;
        }
        return num;
    }

    /**
     * grp srcCell destCell
     *
     * @return the transition probability or -1 in case there is no transition
     * previously recorded or in case the source and destination cells coincide.
     * transition between the cells.
     */
    public double handoverProbability(MobileGroup grp, SmallCell srcCell, SmallCell destCell) {

        if (destCell.equals(srcCell)) {
            return 0;
        }

        double handoffsBetweenCells = getHandoffsBetweenCells(grp, srcCell, destCell);
        double outgoingHandoffs = getHandoffsOutgoing(grp, srcCell);

        if (handoffsBetweenCells == 0
                || outgoingHandoffs == 0) {
            return 0.0;
        }

        double prob = getSimulation().getRandomGenerator().getGaussian(1.0, _probJitter)
                /*robustness testing: intentional random error*/
                * handoffsBetweenCells / outgoingHandoffs;

        return Math.max(Math.min(1.0, prob), 0);// due to jittering
    }

    public SmallCell getCellCenteredAt(Point point) {
        for (SmallCell smallerCell : _smallCells.values()) {
            if (smallerCell.getCoordinates().getX() == point.getX()
                    && smallerCell.getCoordinates().getY() == point.getY()) {
                return smallerCell;
            }
        }
        return null;
    }

    /**
     * _ID
     *
     * @return The SC for this ID or SmallCell.NONE if no cell does with that ID
     * exists (such as negative IDs).
     */
    public SmallCell scByID(Integer _ID) {
        if (_ID < 0) {
            return null;
        }

        return this._smallCells.get(_ID);

    }

    @Override
    /**
     * @return true iff same hashID.
     */
    public boolean equals(Object b) {
        if (b == null) {
            return false;
        }

        if (!(b instanceof CellRegistry)) {
            return false;
        }

        CellRegistry rg = (CellRegistry) b;
        return rg.getSimulation().equals(getSimulation());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 61 * hash + Objects.hashCode(this._sim);
        hash = 61 * hash + Objects.hashCode(this._smallCells);
        return hash;
    }

    /**
     * @return the mobilityModel
     */
    public String getMobilityModel() {
        return _mobilityModel;
    }

    public SmallCell getCellByID(Integer id) {
        return _smallCells.get(id);
    }

    public Map<Integer, Double> getTransitionNeighborsOf(SmallCell _currentlyConnectedSC) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private Set<SmallCell> initSCsRndUniform(
            Area area, Collection<AbstractCachingPolicy> cachingMethods)
            throws CriticalFailureException {
        Scenario s = getSimulation().getScenario();

        try {
            Set<SmallCell> _init_SmallCells_random = new HashSet<>();
            int scsNum = s.intProperty(Space.SC__NUM);
            //<editor-fold defaultstate="collapsed" desc="logging">
            int count = 0;
            double percentage = s.doubleProperty(Simulation.PROGRESS_UPDATE);

            int sum = 0;
            int printPer = (int) (scsNum * percentage);
            printPer = printPer == 0 ? 1 : printPer; // otherwise causes arithmetic exception devide by zero in some cases
            _logger.log(Level.INFO, "Initializing small cells on the area:\n\t{0}/{1}", new Object[]{0, scsNum});
//</editor-fold>

            // try to make it as uniform as possible
            int fromY = 0, toY = 0;
            int fromX = 0, toX = 0;

            for (int i = 0; i < scsNum; i++) {

                fromY = toY;// fromt the prev "to"
                toY = (int) ((i + 1.0) * area.getLengthY() / scsNum);

                fromX = toX;// fromt the prev "to"
                toX = (int) ((i + 1.0) * area.getLengthX() / scsNum);

                Point randCenter = area.getRandPoint(fromY, toY, fromX, toX);
                SmallCell nxt_sc = new SmallCell(getSimulation(), randCenter, area, cachingMethods);
                //<editor-fold defaultstate="collapsed" desc="logging">
                if (++count % printPer == 0) {
                    sum += (int) (10000.0 * printPer / scsNum) / 100;// roiunding, then percent
                    _logger
                            .log(Level.INFO, "\t{0}%", sum);
                }
                _init_SmallCells_random.add(nxt_sc);
//</editor-fold>
            }
            return _init_SmallCells_random;
        } catch (Exception ex) {
            throw new CriticalFailureException(ex);
        }
    }

    private Set<SmallCell> initSCsRnd(
            Area area, Collection<AbstractCachingPolicy> cachingMethods)
            throws CriticalFailureException {

        try {
            Set<SmallCell> _init_SmallCells_random = new HashSet<>();
            int scs_num = _s.intProperty(Space.SC__NUM);
            //<editor-fold defaultstate="collapsed" desc="logging">
            int count = 0;
            double percentage = _s.doubleProperty(Simulation.PROGRESS_UPDATE);

            int sum = 0;
            int printPer = (int) (scs_num * percentage);
            printPer = printPer == 0 ? 1 : printPer; // otherwise causes arithmetic exception devide by zero in some cases
            _logger
                    .log(Level.INFO, "Initializing small cells on the area:\n\t{0}/{1}", new Object[]{0, scs_num});
//</editor-fold>
            for (int i = 0; i < scs_num; i++) {
                Point randCenter = area.getRandPoint();
                SmallCell nxt_sc = new SmallCell(_sim, randCenter, area, cachingMethods);
                //<editor-fold defaultstate="collapsed" desc="logging">
                if (++count % printPer == 0) {
                    sum += (int) (10000.0 * printPer / scs_num) / 100;// roiunding, then percent
                    _logger
                            .log(Level.INFO, "\t{0}%", sum);
                }
                _init_SmallCells_random.add(nxt_sc);
//</editor-fold>
            }
            return _init_SmallCells_random;
        } catch (Exception ex) {
            throw new CriticalFailureException(ex);
        }
    }

    private Set<SmallCell> initSCs(
            Area area, Collection<AbstractCachingPolicy> cachingMethods,
            Point... center) throws CriticalFailureException {

        try {
            Set<SmallCell> _init_SmallCells_random = new HashSet<>();
            int scs_num = _s.intProperty(Space.SC__NUM);
            if (scs_num != center.length) {
                throw new InconsistencyException(
                        "Number of SCs in parameter " + Space.SC__NUM.name() + "= " + scs_num
                        + " does not match the number of SC centers=" + center.length
                );
            }
            //<editor-fold defaultstate="collapsed" desc="logging">
            int count = 0;
            double percentage = _s.doubleProperty(Simulation.PROGRESS_UPDATE);

            int sum = 0;
            int printPer = (int) (scs_num * percentage);
            printPer = printPer == 0 ? 1 : printPer; // otherwise causes arithmetic exception devide by zero in some cases
            _logger
                    .log(Level.INFO, "Initializing small cells on the area:\n\t{0}/{1}", new Object[]{0, scs_num});
            //</editor-fold>
            for (int i = 0; i < scs_num; i++) {
                Point nxtCenter = center[i];
                SmallCell nxtSC = new SmallCell(_sim, nxtCenter, area, cachingMethods);
                //<editor-fold defaultstate="collapsed" desc="log user update">
                boolean logUsrUpdt = ++count % printPer == 0;
                if (logUsrUpdt) {
                    sum += (int) (10000.0 * printPer / scs_num) / 100;// roiunding, then percent
                    _logger
                            .log(Level.INFO, "\t{0}%", sum);
                }
                _init_SmallCells_random.add(nxtSC);
                //</editor-fold>
            }
            return _init_SmallCells_random;
        } catch (Exception ex) {
            throw new CriticalFailureException(ex);
        }
    }

    /**
     * Each line in the trace must be in the following, comma separated textual
     * format, modeling the next small cell to be created: integer coordinate x;
     * integer coordinate y; double radius; double maximum data transmission
     * rate; boolean compute area coverage based on radius length; double
     * backhaul data rate\n
     *
     * sim area
     *
     * @return
     * @throws CriticalFailureException
     */
    private Set<SmallCell> initSCsTrace(
            Area area, Collection<AbstractCachingPolicy> cachingMethods) throws CriticalFailureException {

        int countLines = 0;
        String nxtSCLine = "";

        Scenario s = getSimulation().getScenario();

        String tracePath = s.stringProperty(Space.SC__TRACE_PATH, true);
        _logger.
                log(Level.INFO, "Initializing small cells on the area from trace: {0}",
                        new Object[]{tracePath}
                );

        Set<SmallCell> initFromTrace = new HashSet<>();

        File traceF = null;
        long byteConsumed = 0;
        long traceSize = 0;
        try {
            traceF = (new File(tracePath)).getCanonicalFile();
            //<editor-fold defaultstate="collapsed" desc="checking traceF">
            if (!traceF.exists()) {
                throw new FileNotFoundException("Path to trace file for initializing small "
                        + "cells on the area does not exist: " + traceF.getCanonicalPath());
            }
            if (!traceF.canRead()) {
                throw new IOException("Cannot read from path to trace file for "
                        + "initializing small cells on the area: " + traceF.getCanonicalPath());
            }
            //</editor-fold>
            //<editor-fold defaultstate="collapsed" desc="about logging progress update">
            traceSize = traceF.length(); // rough estimation
            byteConsumed = 0; // howmany bytes consumed from trace file.          
            //</editor-fold>
        } catch (IOException iOException) {
            throw new CriticalFailureException(iOException);
        }

        try (BufferedReader traceR = new BufferedReader(new FileReader(traceF))) {
            while (null != (nxtSCLine = traceR.readLine())) {
                countLines++;

                if (nxtSCLine.startsWith("#") || nxtSCLine.isEmpty()) {
                    continue; // ignore comments
                }

                String nxtSC_descr;
                //<editor-fold defaultstate="collapsed" desc="get rid of comments in line">
                StringTokenizer commnetTok = new StringTokenizer(nxtSCLine, "#");
                if (commnetTok.hasMoreTokens()) {
                    nxtSC_descr = commnetTok.nextToken(); // the first token is the one we want
                } else {
                    nxtSC_descr = nxtSCLine;
                }
                //</editor-fold>
                StringTokenizer tokens = new StringTokenizer(nxtSC_descr, ";");

                Point center = null;
                double radius = 0;
                String neighbors;

                int id = Integer.parseInt(tokens.nextToken().trim());

                int x = Integer.parseInt(tokens.nextToken().trim());
                int y = Integer.parseInt(tokens.nextToken().trim());
                center = new Point(x, y);

                radius = Double.parseDouble(tokens.nextToken().trim());
                if (radius == -1) {
                    radius
                            = _sim.getRandomGenerator().getGaussian(
                                    s.doubleProperty(Space.SC__RADIUS__MEAN),
                                    s.doubleProperty(Space.SC__RADIUS__STDEV)
                            );
                }

                neighbors = tokens.nextToken().trim();
                Map<Integer, Double> cellID_probs = null;
                if (!(neighbors.equals(Values.UNDEFINED)
                        || neighbors.equals(Values.NULL)
                        || neighbors.equals(Values.NONE)
                        || neighbors.equals(""))) {
                    cellID_probs = initSCsTraceTokenizeNeighbs(neighbors);
                }

                long capacity = utils.CommonFunctions.parseSizeToBytes(s.stringProperty(Space.SC__BUFFER__SIZE, false));

                SmallCell nxt_sc = new SmallCell(
                        id, _sim, center, radius,
                        area, cellID_probs, cachingMethods, capacity
                );

                initFromTrace.add(nxt_sc);

                //<editor-fold defaultstate="collapsed" desc="logging progress">
                byteConsumed += nxtSCLine.length() * 2; //16 bit chars
                int progress = (int) (10000.0 * byteConsumed / traceSize) / 100;// rounding, then percent
                _logger.log(Level.INFO, "\t{0}%", progress);
                _logger.log(Level.FINE, "\tSmall Cell created: {0}", nxt_sc.toSynopsisString());
                //</editor-fold>
            }

            _logger.log(Level.INFO, "Finished. Total small cells created: {0}", initFromTrace.size());
            return initFromTrace;
        } catch (InvalidOrUnsupportedException | CriticalFailureException | IOException | NumberFormatException | NoSuchElementException ex) {
            String msg = "";
            if (ex.getClass() == NumberFormatException.class
                    || ex.getClass() == NoSuchElementException.class) {
                msg = "Trace file is mulformed at line "
                        + countLines
                        + ":\n\t"
                        + nxtSCLine;
            }
            throw new CriticalFailureException(msg, ex);
        }
    }

    /**
     * Initializes Cells. Cells are added to the area by the constructor of
     * SmallCell.
     *
     * sim area cachingPolicies
     *
     * @return
     * @throws exceptions.CriticalFailureException
     */
    public Set<SmallCell> createSCs(Area area,
            Collection<AbstractCachingPolicy> cachingPolicies) throws CriticalFailureException {
        Scenario s = getSimulation().getScenario();

        Set<SmallCell> scSet = null;
        try {
            List<String> scsInit = s.listOfStringsProperty(Space.SC__INIT, false);
            if (scsInit.isEmpty()) {
                _logger.log(Level.WARNING, "There are no small cells defined in property {0}", Space.SC__INIT.name());
            }
            if (scsInit.size() == 1) {

                switch (scsInit.get(0).toLowerCase()) {

                    case Values.RANDOM:
                        scSet = initSCsRnd(area, cachingPolicies);
                        break;
                    case Values.RANDOM_UNIFORM:
                        scSet = initSCsRndUniform(area, cachingPolicies);
                        break;

                    case Values.TRACE:
                        scSet = initSCsTrace(area, cachingPolicies);
                        break;

                    default:
                        Point center = area.getPoint(scsInit.get(0));
                        scSet = initSCs(area, cachingPolicies, center);
                        break;

                }

            } else {

                Point[] centers = area.getPoints(scsInit);
                scSet = initSCs(area, cachingPolicies, centers);

            }

            //<editor-fold defaultstate="collapsed" desc="discover neighbors sanity check">
            if (_sim.getCachingStrategies().contains(Values.CACHING__NAIVE__TYPE03)
                    && s.intProperty(Space.SC__WARMUP_PERIOD) < 100) {
                throw new CriticalFailureException(Values.CACHING__NAIVE__TYPE03 + " is enabled. Finding neighbors for each SC is mandatory."
                        + " But the time interval for discovering neighbors is too small: "
                        + s.intProperty(Space.SC__WARMUP_PERIOD));
            }
            //</editor-fold>

            return scSet;
        } catch (CriticalFailureException | UnsupportedOperationException ex) {
            throw new CriticalFailureException(ex);
        }

    }

    private Map<Integer, Double> initSCsTraceTokenizeNeighbs(String neighbors) {
        StringTokenizer tokens = new StringTokenizer(neighbors, ",");
        Map<Integer, Double> toReturnMap = new HashMap<>(tokens.countTokens() / 2);

        while (tokens.hasMoreElements()) {
            int id = Integer.parseInt(tokens.nextToken());
            double prob = Double.parseDouble(tokens.nextToken());

            toReturnMap.put(id, prob);
        }

        return toReturnMap;
    }

}
