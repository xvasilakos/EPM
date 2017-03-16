package sim.space.cell;

import app.properties.Simulation;
import app.properties.Space;
import app.properties.valid.Values;
import caching.base.AbstractCachingModel;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
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
public final class CellRegistry implements ISimulationMember, ISynopsisString {

    private final SimulationBaseRunner sim;
    private final Scenario scenario;
    private final Logger LOG;

    /**
     * key: ID of SC
     *
     * Value: SmallCell with the given ID
     */
    private final Map<Integer, SmallCell> smallCells;
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
    private final String mobModel;
    private final MobileGroupsRegistry muGroupRegistry;
    private final double probJitter;

    public CellRegistry(
            SimulationBaseRunner simulation, MobileGroupsRegistry groupRegistry,
            MacroCell mc, Area area) throws InvalidOrUnsupportedException {

        sim = simulation;
        scenario = sim.getScenario();
        LOG = CommonFunctions.getLoggerFor(CellRegistry.class, "simID=" + getSimulation().getID());

        mobModel = scenario.stringProperty(Space.MOBILITY_MODEL, false);
        probJitter = scenario.doubleProperty(Space.SC__HANDOFF_PROBABILITY__STDEV);

        muGroupRegistry = groupRegistry;

        smallCells = new TreeMap<>();
        Collection<SmallCell> scs = createSCs(area, simulation.getCachingStrategies());
        Iterator<SmallCell> cellsIter = scs.iterator();
        while (cellsIter.hasNext()) {
            SmallCell sc = cellsIter.next();
            int scID = sc.getID();
            this.smallCells.put(scID, sc);
        }

        this._macroCell = mc;

    }

    @Override
    public String toString() {
        return this.sim.toString() + " "
                + this.muGroupRegistry.toString() + " "
                + "; Cells: <" + CommonFunctions.toString(smallCells)
                + ">; macrocell: " + _macroCell.toString()
                + "; mobility model: " + mobModel
                + "; probability jitter: " + probJitter;
    }

    public String toSynopsisString() {
        return this.sim.toString() + " "
                + this.muGroupRegistry.toSynopsisString() + " "
                + "; Cells: <" + CommonFunctions.toString(smallCells)
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
        return Collections.unmodifiableCollection(smallCells.values());
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
        int rnd = sim.getRandomGenerator().randIntInRange(0, size - 1);
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
        int rndPos = sim.getRandomGenerator().randIntInRange(0, lastPos);
        return coveringSmallerCells.get(rndPos);
    }

    @Override
    public final int simID() {
        return sim.getID();
    }

    @Override
    public final SimulationBaseRunner getSimulation() {
        return sim;
    }

    @Override
    public final int simTime() {
        return sim.simTime();
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
        switch (mobModel) {
            case Values.LOCATION:
                comingFromID = comingFrom.getID() + "";
                residenceID = residenceSC.getID() + "";
                break;

            case Values.LOCATION__PLUS__GROUP:
                comingFromID = CommonFunctions.combineCellMUGroup(comingFrom, grp == null ? - 1 : grp.getId());
                residenceID = CommonFunctions.combineCellMUGroup(residenceSC, grp == null ? - 1 : grp.getId());
                break;

            default:
                throw new UnsupportedOperationException("Unknonwn or unsupported model " + mobModel
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
        switch (mobModel) {
            case Values.LOCATION:
                disconFromID = disconFrom.getID() + "";
                conToID = conTo.getID() + "";
                break;

            case Values.LOCATION__PLUS__GROUP:
                disconFromID = CommonFunctions.combineCellMUGroup(disconFrom, grp == null ? - 1 : grp.getId());
                conToID = CommonFunctions.combineCellMUGroup(conTo, grp == null ? - 1 : grp.getId());
                break;

            default:
                throw new UnsupportedOperationException("Unknonwn or unsupported model " + mobModel
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
        switch (mobModel) {
            case Values.LOCATION:
                srcID = src.getID() + "";
                destID = dest.getID() + "";
                break;

            case Values.LOCATION__PLUS__GROUP:
                srcID = CommonFunctions.combineCellMUGroup(src, grp == null ? - 1 : grp.getId());
                destID = CommonFunctions.combineCellMUGroup(dest, grp == null ? - 1 : grp.getId());
                break;

            default:
                throw new UnsupportedOperationException("Unknonwn or unsupported model " + mobModel
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
        switch (mobModel) {

            case Values.LOCATION:
                srcID = src.getID() + "";
                destID = dest.getID() + "";
                break;

            case Values.LOCATION__PLUS__GROUP:
                srcID = CommonFunctions.combineCellMUGroup(src, grp == null ? - 1 : grp.getId());
                destID = CommonFunctions.combineCellMUGroup(dest, grp == null ? - 1 : grp.getId());
                break;

            default:
                throw new UnsupportedOperationException("Unknonwn or unsupported model " + mobModel
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
        switch (mobModel) {

            case Values.LOCATION:
                comingFromSCID = fromSC.getID() + "";
                residentSCID = residentSC.getID() + "";
                break;

            case Values.LOCATION__PLUS__GROUP:
                comingFromSCID = CommonFunctions.combineCellMUGroup(fromSC, grp == null ? - 1 : grp.getId());
                residentSCID = CommonFunctions.combineCellMUGroup(residentSC, grp == null ? - 1 : grp.getId());
                break;

            default:
                throw new UnsupportedOperationException("Unknonwn or unsupported model " + mobModel
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
        switch (mobModel) {

            case Values.LOCATION:
                disconSCID = disconSC.getID() + "";
                connSCID = conToSC.getID() + "";
                break;

            case Values.LOCATION__PLUS__GROUP:
                disconSCID = CommonFunctions.combineCellMUGroup(disconSC, grp == null ? - 1 : grp.getId());
                connSCID = CommonFunctions.combineCellMUGroup(conToSC, grp == null ? - 1 : grp.getId());
                break;

            default:
                throw new UnsupportedOperationException("Unknonwn or unsupported model " + mobModel
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
        switch (mobModel) {

            case Values.LOCATION:
                srcID = src.getID() + "";
                break;

            case Values.LOCATION__PLUS__GROUP:
                srcID = CommonFunctions.combineCellMUGroup(src, grp == null ? - 1 : grp.getId());
                break;

            default:
                throw new UnsupportedOperationException("Unknonwn or unsupported model " + mobModel
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
        switch (mobModel) {

            case Values.LOCATION:
                destID = dest.getID() + "";
                break;

            case Values.LOCATION__PLUS__GROUP:
                destID = CommonFunctions.combineCellMUGroup(dest, grp == null ? - 1 : grp.getId());
                break;

            default:
                throw new UnsupportedOperationException("Unknonwn or unsupported model " + mobModel
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

        double prob = getSimulation().getRandomGenerator().getGaussian(1.0, probJitter)
                /*robustness testing: intentional random error*/
                * handoffsBetweenCells / outgoingHandoffs;

        return Math.max(Math.min(1.0, prob), 0);// due to jittering
    }

    public SmallCell getCellCenteredAt(Point point) {
        for (SmallCell smallerCell : smallCells.values()) {
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

        return this.smallCells.get(_ID);

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
        hash = 61 * hash + Objects.hashCode(this.sim);
        hash = 61 * hash + Objects.hashCode(this.smallCells);
        return hash;
    }

    /**
     * @return the mobilityModel
     */
    public String getMobModel() {
        return mobModel;
    }

    public SmallCell getCellByID(Integer id) {
        return smallCells.get(id);
    }

    public Map<Integer, Double> getTransitionNeighborsOf(SmallCell _currentlyConnectedSC) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private List<SmallCell> initSCsRndUniform(
            Area area, Collection<AbstractCachingModel> cachingMethods)
            throws CriticalFailureException {
        Scenario s = getSimulation().getScenario();

        try {
            List<SmallCell> scsRndUnif = new ArrayList<>();
            int scsNum = s.intProperty(Space.SC__NUM);
            //<editor-fold defaultstate="collapsed" desc="logging">
            int count = 0;
            double percentage = s.doubleProperty(Simulation.PROGRESS_UPDATE);

            int sum = 0;
            int printPer = (int) (scsNum * percentage);
            printPer = printPer == 0 ? 1 : printPer; // otherwise causes arithmetic exception devide by zero in some cases
            LOG.log(Level.INFO, "Initializing small cells on the area:\n\t{0}/{1}", new Object[]{0, scsNum});
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
                    LOG
                            .log(Level.INFO, "\t{0}%", sum);
                }
                scsRndUnif.add(nxt_sc);
//</editor-fold>
            }
            return scsRndUnif;
        } catch (Exception ex) {
            throw new CriticalFailureException(ex);
        }
    }

    private List<SmallCell> initSCsRnd(
            Area area, Collection<AbstractCachingModel> cachingMethods)
            throws CriticalFailureException {

        try {
            List<SmallCell> scsRnd = new ArrayList<>();
            int scs_num = scenario.intProperty(Space.SC__NUM);
            //<editor-fold defaultstate="collapsed" desc="logging">
            int count = 0;
            double percentage = scenario.doubleProperty(Simulation.PROGRESS_UPDATE);

            int sum = 0;
            int printPer = (int) (scs_num * percentage);
            printPer = printPer == 0 ? 1 : printPer; // otherwise causes arithmetic exception devide by zero in some cases
            LOG
                    .log(Level.INFO, "Initializing small cells on the area:\n\t{0}/{1}", new Object[]{0, scs_num});
//</editor-fold>
            for (int i = 0; i < scs_num; i++) {
                Point randCenter = area.getRandPoint();
                SmallCell nxt_sc = new SmallCell(sim, randCenter, area, cachingMethods);
                //<editor-fold defaultstate="collapsed" desc="logging">
                if (++count % printPer == 0) {
                    sum += (int) (10000.0 * printPer / scs_num) / 100;// roiunding, then percent
                    LOG
                            .log(Level.INFO, "\t{0}%", sum);
                }
                scsRnd.add(nxt_sc);
//</editor-fold>
            }
            return scsRnd;
        } catch (Exception ex) {
            throw new CriticalFailureException(ex);
        }
    }

    private List<SmallCell> initSCs(
            Area area, Collection<AbstractCachingModel> cachingMethods,
            Point... center) throws CriticalFailureException {

        try {
            List<SmallCell> initSCs = new ArrayList<>();
            int scsNum = scenario.intProperty(Space.SC__NUM);
            if (scsNum != center.length) {
                throw new InconsistencyException(
                        "Number of SCs in parameter " + Space.SC__NUM.name() + "= " + scsNum
                        + " does not match the number of SC centers=" + center.length
                );
            }
//<editor-fold defaultstate="collapsed" desc="logging">
            int count = 0;
            double percentage = scenario.doubleProperty(Simulation.PROGRESS_UPDATE);

            int sum = 0;
            int printPer = (int) (scsNum * percentage);
            printPer = printPer == 0 ? 1 : printPer; // otherwise causes arithmetic exception devide by zero in some cases
            LOG
                    .log(Level.INFO, "Initializing small cells on the area:\n\t{0}/{1}", new Object[]{0, scsNum});
//</editor-fold>

            for (int i = 0; i < scsNum; i++) {
                Point nxtCenter = center[i];
                SmallCell nxtSC = new SmallCell(sim, nxtCenter, area, cachingMethods);

//<editor-fold defaultstate="collapsed" desc="log user update">
                boolean logUsrUpdt = ++count % printPer == 0;
                if (logUsrUpdt) {
                    sum += (int) (10000.0 * printPer / scsNum) / 100;// roiunding, then percent
                    LOG
                            .log(Level.INFO, "\t{0}%", sum);
                }
                initSCs.add(nxtSC);
//</editor-fold>

            }
            return initSCs;
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
    private List<SmallCell> initSCsTrace(
            Area area, Collection<AbstractCachingModel> cachingMethods
    ) throws CriticalFailureException {

        Area.RealArea theRealDimensions = area.getREAL_AREA();
        int minX = theRealDimensions.minX,
                minY = theRealDimensions.minY,
                maxX = theRealDimensions.maxX,
                maxY = theRealDimensions.maxY;

//        String metadataPath = s.stringProperty(Space.SC__TRACE_METADATA_PATH, true);
//        File metaF = (new File(metadataPath)).getAbsoluteFile();
//        Couple<Point, Point> areaDimensions = Cells.extractAreaFromMetadata(metaF, minX, minY, maxX, maxY);
//        
//        minX = areaDimensions.getFirst().getX();
//        minY = areaDimensions.getFirst().getY();
//        maxX = areaDimensions.getSecond().getX();
//        maxY = areaDimensions.getSecond().getY();
        String nxtLn = "";

        Scenario s = getSimulation().getScenario();

        String tracePath = s.stringProperty(Space.SC__TRACE_PATH, true);

        double meanR = s.doubleProperty(Space.SC__RADIUS__MEAN);
        double stdevR = s.doubleProperty(Space.SC__RADIUS__STDEV);

        LOG.log(Level.INFO, "Initializing small cells on the area with mean "
                + "radius size {0} (std.dev. {1}) based on trace: \"{2}\". ",
                new Object[]{
                    meanR,
                    stdevR,
                    tracePath
                }
        );

        List<SmallCell> initFromTrace = new ArrayList<>();

        File traceF = null;
        long byteConsumed = 0;
        long traceSize = 0;
        try {
            traceF = (new File(tracePath)).getCanonicalFile();
            traceSize = traceF.length();

            if (!traceF.exists()) {
                throw new FileNotFoundException("Path to file for initializing small "
                        + "cells not exist: "
                        + "\""
                        + traceF.getCanonicalPath()
                        + "\"");
            }

        } catch (IOException iOException) {
            throw new CriticalFailureException(iOException);
        }

        int countLines = 0;
        try (BufferedReader traceR = new BufferedReader(new FileReader(traceF))) {
            while (null != (nxtLn = traceR.readLine())) {
                countLines++;

                if (nxtLn.startsWith("#") || nxtLn.isEmpty()) {
                    continue; // ignore comments
                }

                String[] tokens = nxtLn.split(" ");

                Point center = null;

                int id = (int) Double.parseDouble(tokens[0]);

                int x = (int) Double.parseDouble(tokens[1]);
                if (x > maxX || x < minX) {
                    throw new InconsistencyException(
                            "Cell with id " + id + " has dimension x="
                            + x + " out of bounds:"
                            + " ["
                            + minX
                            + ","
                            + maxX
                            + "]."
                    );
                }

                int y = (int) Double.parseDouble(tokens[2]);
                if (y > maxY || y < minY) {
                    throw new InconsistencyException(
                            "Cell with id " + id + " has dimension y="
                            + y + " out of bounds:"
                            + " ["
                            + minY
                            + ","
                            + maxY
                            + "]."
                    );
                }

                // use relative dimensions as area will be translated 
                // relatively to starting point [0,0]
                center = new Point(x - minX, y - minY);

                //use a random radius ..
                int radius = (int) sim.getRandomGenerator().
                        getGaussian(meanR, stdevR);

                long capacity = utils.CommonFunctions.parseSizeToBytes(
                        s.stringProperty(Space.SC__BUFFER__SIZE, false));

                SmallCell newSC = new SmallCell(
                        id, sim, center, radius,
                        area, cachingMethods, capacity
                );

                initFromTrace.add(newSC);

                //<editor-fold defaultstate="collapsed" desc="logging progress">
                byteConsumed += nxtLn.length() * 2; //16 bit chars
                int progress = (int) (10000.0 * byteConsumed / traceSize) / 100;// rounding, then percent
                LOG.log(Level.INFO, "\t{0}%", progress);
                LOG.log(Level.INFO, "\tSmall Cell created: {0}", newSC.toSynopsisString());
                //</editor-fold>
            }

            LOG.log(Level.INFO, "Finished. {0} small cells created: {1}",
                    new Object[]{initFromTrace.size(), CommonFunctions.toString(initFromTrace)}
            );

            


            return initFromTrace;
        } catch (InvalidOrUnsupportedException | CriticalFailureException | IOException | NumberFormatException | NoSuchElementException ex) {
            String msg = "";
            if (ex.getClass() == NumberFormatException.class
                    || ex.getClass() == NoSuchElementException.class) {
                msg = "Trace file is mulformed at line "
                        + countLines
                        + ":\n\t"
                        + nxtLn;
            }
            throw new CriticalFailureException(msg, ex);
        }
    }

    /**
     * Initializes Cells. Cells are added to the area by the constructor of
     * SmallCell.
     *
     * @param area
     * @param cachingPolicies
     * @return
     * @throws CriticalFailureException
     */
    public List<SmallCell> createSCs(Area area,
            Collection<AbstractCachingModel> cachingPolicies) throws CriticalFailureException {
        Scenario s = getSimulation().getScenario();

        List<SmallCell> theSCs = null;
        try {
            List<String> scsInit = s.listOfStringsProperty(Space.SC__INIT, false);
            if (scsInit.isEmpty()) {
                LOG.log(Level.WARNING,
                        "There are no small cells defined in property {0}",
                        Space.SC__INIT.name());
            }
            if (scsInit.size() == 1) {

                switch (scsInit.get(0).toUpperCase()) {

                    case Values.RANDOM:
                        theSCs = initSCsRnd(area, cachingPolicies);
                        break;
                    case Values.RANDOM_UNIFORM:
                        theSCs = initSCsRndUniform(area, cachingPolicies);
                        break;

                    case Values.TRACE:
                        theSCs = initSCsTrace(area, cachingPolicies);
                        break;

                    default:
                        throw new UnsupportedOperationException(scsInit.get(0));
//@todo most probably delete. No need to keep:
//                        Point center = area.getPoint(scsInit.get(0));
//                        scSet = initSCs(area, cachingPolicies, center);
//                        break;
                }
            } else {
                Point[] centers = area.getPoints(scsInit);
                theSCs = initSCs(area, cachingPolicies, centers);
            }

            //discover neighbors sanity check
            if (sim.getCachingStrategies().contains(Values.CACHING__NAIVE__TYPE03)
                    && s.intProperty(Space.SC__WARMUP_PERIOD) < 100) {
                throw new CriticalFailureException(Values.CACHING__NAIVE__TYPE03 + " is enabled."
                        + " Finding neighbors for each SC is mandatory."
                        + " But the time interval for discovering neighbors is too small: "
                        + s.intProperty(Space.SC__WARMUP_PERIOD));
            }

            double areaSz = area.size();
            double coveredSz = 0;
            for (SmallCell nxtSC : theSCs) {
                coveredSz += nxtSC.CoverageSize();
            }
            
            double percentage = ((int)(10000.0 * coveredSz/areaSz))/100.0;
            LOG.log(Level.INFO, "Created cells cover {0}% of the area.", percentage);
            
            return theSCs;
        } catch (UnsupportedOperationException ex) {
            throw new CriticalFailureException(ex);
        }
    }

}
