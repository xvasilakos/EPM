package sim.space.cell;

import app.properties.Simulation;
import app.properties.Space;
import app.properties.valid.Values;
import caching.base.AbstractCachingModel;
import exceptions.CriticalFailureException;
import exceptions.InconsistencyException;
import exceptions.InvalidOrUnsupportedException;
import exceptions.WrongOrImproperArgumentException;
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
    private final TraceCellsLoader traceCellsLoader;

    public CellRegistry(
            SimulationBaseRunner simulation, MobileGroupsRegistry groupRegistry,
            MacroCell mc, Area area) throws InvalidOrUnsupportedException {

        sim = simulation;
        scenario = sim.getScenario();
        LOG = CommonFunctions.getLoggerFor(CellRegistry.class, "simID=" + getSimulation().getID());

        mobModel = scenario.stringProperty(Space.MOBILITY_MODEL);
        probJitter = scenario.doubleProperty(Space.SC__HANDOFF_PROBABILITY__STDEV);

        muGroupRegistry = groupRegistry;

        smallCells = new TreeMap<>();

        traceCellsLoader = new TraceCellsLoader();

        this._macroCell = mc;

        Collection<SmallCell> scs = createSCs(area, simulation.getCachingModels());
        Iterator<SmallCell> cellsIter = scs.iterator();
        while (cellsIter.hasNext()) {
            SmallCell sc = cellsIter.next();
            int scID = sc.getID();
            this.smallCells.put(scID, sc);
        }

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

    @Override
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
     *
     * @param mu
     * @param src
     * @param dest
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
     * Returns probability based on srcCell destCell and -if using a mobile
     * transitions history- the mobile user's group.
     *
     * Tries to use a probability matrix, which indicates the use of a trace for
     * cells. If there is no known probability matrix, then probabilities are
     * computed on the fly based on a history of mobile transitions between the
     * cells. Note, that in the latter case, the history utilises also the grp
     * parameter.
     *
     * @param mu the mobile user
     * @param srcCell the source cell
     * @param destCell the possible destination cell
     * @return the expected mobile transition probability between the source and
     * the destination cells
     *
     */
    public double handoverProbability(MobileUser mu, SmallCell srcCell, SmallCell destCell) {

        String key = traceCellsLoader.hashKey(srcCell, destCell);

        double probMarkov = traceCellsLoader.probMatrixGeneral.get(key);

        double lastSojournTime = mu.getLastSojournTime();

        if (true) {//xxx remove that testing if(true); uncomment the next if
//        if (probMatrix() != null) {

            if (50 < lastSojournTime && lastSojournTime < 100) {
                return probMarkov * traceCellsLoader.probMatrix_rt50to100.get(key);
            } else if (100 < lastSojournTime && lastSojournTime < 200) {
                return probMarkov * traceCellsLoader.probMatrix_rt100to200.get(key);
            } else if (200 < lastSojournTime && lastSojournTime < 300) {
                return probMarkov * traceCellsLoader.probMatrix_rt200to300.get(key);
            } else if (300 < lastSojournTime) {
                return probMarkov * traceCellsLoader.probMatrix_rtover300.get(key);
            } else {
                return probMarkov * traceCellsLoader.probMatrix_rtunder50.get(key);
            }

        }

//        if (destCell.equals(srcCell)) {
//            return 0;
//        }
        MobileGroup grp = mu.getUserGroup();

        double handoffsBetweenCells = getHandoffsBetweenCells(grp, srcCell, destCell);
        double outgoingHandoffs = getHandoffsOutgoing(grp, srcCell);

        if (handoffsBetweenCells == 0
                || outgoingHandoffs == 0) {
            return 0.0;
        }

        // TODO the folowing makes sence only when using a fixed/synthetic/artificial mobilty model 
        //double prob = getSimulation().getRandomGenerator().getGaussian(1.0, probJitter)
        //        /*robustness testing: intentional random error*/
        //        * handoffsBetweenCells / outgoingHandoffs;
        // return Math.max(Math.min(1.0, prob), 0);// due to probability jittering
        double prob = handoffsBetweenCells / outgoingHandoffs;

        return Math.max(Math.min(1.0, prob), 0);// due to probability jittering
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
            Area area, Collection<AbstractCachingModel> cacheMdls)
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
                SmallCell nxt_sc = new SmallCell(getSimulation(), randCenter, area, cacheMdls);
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
            Area area, Collection<AbstractCachingModel> cacheMdls)
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
                SmallCell nxt_sc = new SmallCell(sim, randCenter, area, cacheMdls);
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
            Area area, Collection<AbstractCachingModel> cacheMdls,
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
                SmallCell nxtSC = new SmallCell(sim, nxtCenter, area, cacheMdls);

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
            List<String> scsInit = s.listOfStringsProperty(Space.SC__INIT);
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
                        theSCs = traceCellsLoader.initSCsTrace(scenario, area, cachingPolicies);
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
            if (sim.getCachingModels().contains(Values.CACHING__NAIVE__TYPE03)
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

            double percentage = ((int) (10000.0 * coveredSz / areaSz)) / 100.0;
            LOG.log(Level.INFO, "Created cells cover {0}% of the area.", percentage);

            return theSCs;
        } catch (UnsupportedOperationException | InconsistencyException | IOException | WrongOrImproperArgumentException ex) {
            throw new CriticalFailureException(ex);
        }
    }

    public class TraceCellsLoader {

        /**
         * " Probability Matrix.csv" " rt50to100.csv" " rt100to200.csv" "
         * rt200to300.csv" " rtover300" " rtunder50"
         */
        List<String> suffixes;
        Map<String, Double> probMatrixGeneral = new HashMap<>();
        Map<String, Double> probMatrix_rt50to100 = new HashMap<>();
        Map<String, Double> probMatrix_rt100to200 = new HashMap<>();
        Map<String, Double> probMatrix_rt200to300 = new HashMap<>();
        Map<String, Double> probMatrix_rtover300 = new HashMap<>();
        Map<String, Double> probMatrix_rtunder50 = new HashMap<>();

        private TraceCellsLoader() {
            this.suffixes = new ArrayList();
            suffixes.add(" Probability Matrix.csv");
            suffixes.add(" rt50to100.csv");
            suffixes.add(" rt100to200.csv");
            suffixes.add(" rt200to300.csv");
            suffixes.add(" rtover300.csv");
            suffixes.add(" rtunder50.csv");

        }

        /**
         * Each line in the trace must be in the following, comma separated
         * textual format, modeling the next small cell to be created: integer
         * coordinate x; integer coordinate y; double radius; double maximum
         * data transmission rate; boolean compute area coverage based on radius
         * length; double backhaul data rate\n
         *
         *
         * @param s the scenario for extracting the parameters needed
         * @param area the simulation area
         * @param cacheMdls
         * @return
         * @throws CriticalFailureException
         */
        private List<SmallCell> initSCsTrace(Scenario s,
                Area area, Collection<AbstractCachingModel> cacheMdls
        ) throws CriticalFailureException, InconsistencyException, IOException, FileNotFoundException, WrongOrImproperArgumentException {

            Area.RealArea theRealDimensions = area.getREAL_AREA();
            int minX = theRealDimensions.minX,
                    minY = theRealDimensions.minY,
                    maxX = theRealDimensions.maxX,
                    maxY = theRealDimensions.maxY;

//        String metadataPath = s.stringProperty(Space.SC__TRACE_METADATA, true);
//        File metaF = (new File(metadataPath)).getAbsoluteFile();
//        Couple<Point, Point> areaDimensions = Cells.extractAreaFromMetadata(metaF, minX, minY, maxX, maxY);
//        
//        minX = areaDimensions.getFirst().getX();
//        minY = areaDimensions.getFirst().getY();
//        maxX = areaDimensions.getSecond().getX();
//        maxY = areaDimensions.getSecond().getY();
            String nxtLn = "";

//            = CellRegistry.this.getSimulation().getScenario();
            String tracePath = s.stringProperty(Space.SC__TRACE_BASE)
                    + "/" + s.stringProperty(Space.SC__TRACE);

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

            List<SmallCell> initialisedFromTrc = null;

            initialisedFromTrc = new ArrayList<>();
            parseTraceFile(tracePath, nxtLn, maxX, minX, maxY, minY, meanR, stdevR, s, area, cacheMdls, initialisedFromTrc);

            for (String suffix : suffixes) {
                String probsPath = s.stringProperty(Space.SC__TRACE_BASE)
                        + "/" + s.stringProperty(Space.SC__TRACE__PROB_MATRIX)
                        + suffix;
                
                System.out.println("probsPath="+probsPath);
        System.out.println("SC__TRACE__PROB_MATRIX="+s.stringProperty(Space.SC__TRACE__PROB_MATRIX));
        System.exit(-66);
                
                parseProbsMatrix(suffix, probsPath, initialisedFromTrc);
            }

            return initialisedFromTrc;

        }

        /**
         * *
         * Parses the trace file.
         *
         * @param traceF
         * @param nxtLn
         * @param maxX
         * @param minX
         * @param maxY
         * @param minY
         * @param meanR
         * @param stdevR
         * @param scellsParsed@param area
         * @param cacheMdls
         * @param initFromTrace
         * @param byteConsumed
         * @param sz
         * @return
         * @throws CriticalFailureException
         * @throws InconsistencyException
         */
        private List<SmallCell> parseTraceFile(
                String tracePath, String nxtLn,
                int maxX, int minX, int maxY, int minY,
                double meanR, double stdevR, Scenario s,
                Area area, Collection<AbstractCachingModel> cacheMdls,
                List<SmallCell> cellsParsed) throws CriticalFailureException, InconsistencyException, FileNotFoundException, IOException {

            File cellsTraceF = null;
            long byteConsumed = 0;
            long sz = 0;
            cellsTraceF = (new File(tracePath)).getCanonicalFile();

            sz = cellsTraceF.length();

            if (!cellsTraceF.exists()) {
                throw new FileNotFoundException("Path to file for initializing small "
                        + "cells not exist: "
                        + "\""
                        + cellsTraceF.getCanonicalPath()
                        + "\"");
            }

            int countLines = 0;
            try (
                    BufferedReader rdr = new BufferedReader(new FileReader(tracePath))) {
                while (null != (nxtLn = rdr.readLine())) {
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

                    long capacity = utils.CommonFunctions.parseSizeToBytes(s.stringProperty(Space.SC__BUFFER__SIZE));

                    SmallCell newSC = new SmallCell(
                            id, sim, center, radius,
                            area, cacheMdls, capacity
                    );

                    cellsParsed.add(newSC);

                    //<editor-fold defaultstate="collapsed" desc="logging progress">
                    byteConsumed += nxtLn.length() * 2; //16 bit chars
                    int progress = (int) (10000.0 * byteConsumed / sz) / 100;// rounding, then percent
                    LOG.log(Level.INFO, "\t{0}%", progress);
                    LOG.log(Level.INFO, "\tSmall Cell created: {0}", newSC.toSynopsisString());
                    //</editor-fold>
                }

                LOG.log(Level.INFO, "Finished. {0} small cells created: {1}",
                        new Object[]{cellsParsed.size(), CommonFunctions.toString(cellsParsed)}
                );

                return cellsParsed;
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

        private List<SmallCell> parseProbsMatrix(String suffix,
                String probsPath, List<SmallCell> cellsParsed)
                throws CriticalFailureException, InconsistencyException, FileNotFoundException, IOException, WrongOrImproperArgumentException {

            File probsMatrixF = (new File(probsPath)).getCanonicalFile();
            if (!probsMatrixF.exists()) {
                throw new FileNotFoundException("Path to probability cells' matrix file for initializing small "
                        + "cells does not exist: "
                        + "\""
                        + probsMatrixF.getCanonicalPath()
                        + "\"");
            }

            Map<String, Double> probMatrix;

            switch (suffix) {
                case " Probability Matrix.csv":
                    probMatrix = this.probMatrixGeneral;
                    break;
                case " rt50to100.csv":
                    probMatrix = this.probMatrix_rt50to100;
                    break;
                case " rt100to200.csv":
                    probMatrix = this.probMatrix_rt100to200;
                    break;
                case " rt200to300.csv":
                    probMatrix = this.probMatrix_rt200to300;
                    break;
                case " rtover300.csv":
                    probMatrix = this.probMatrix_rtover300;
                    break;
                case " rtunder50.csv":
                    probMatrix = this.probMatrix_rtunder50;
                    break;
                default:
                    throw new exceptions.WrongOrImproperArgumentException("Unknown type of file: \"" + suffix + "\"");
            }

            initProbMatrix(cellsParsed, probMatrix);

            int countLines = 0;
            String nxtLn = null;

            try (BufferedReader reader = new BufferedReader(
                    new FileReader(probsMatrixF)
            )) {

                // ignore first line
                if (null == (nxtLn = reader.readLine())) {
                    throw new InconsistencyException("Title line in \"" + probsMatrixF.getCanonicalPath() + "\" is empty");
                }

                while (null != (nxtLn = reader.readLine())) {

                    SmallCell nxtCl = cellsParsed.get(countLines++);

                    String[] tokens = nxtLn.split(",");

                    if (tokens.length != cellsParsed.size() + 1) {
                        throw new InconsistencyException(""
                                + "Line #" + countLines
                                + " in \"" + probsMatrixF.getCanonicalPath() + "\""
                                + " has wrong number of tokens = " + tokens.length
                                + ". The expected number of tokens is equal to the cell ID and the number of all cells, i.e: "
                                + (cellsParsed.size() + 1)
                        );
                    }

                    int id = (int) Double.parseDouble(tokens[0].replace("\"", "")); //TODO ignore it for now; pending to fix in future by mohammad
                    for (int i = 1; i < tokens.length; i++) {
                        double prob = Double.parseDouble(tokens[i]);
                        SmallCell nxtNeigh = cellsParsed.get(i - 1);
                        nxtCl.addNeighbor(nxtNeigh);
                        probMatrix.put(hashKey(nxtCl, nxtNeigh), prob);
                    }

                }

                LOG.log(Level.INFO, "Finished. Small cells probability matrix is: \n{0}",
                        new Object[]{CommonFunctions.toString(probMatrix)}
                );

                return cellsParsed;
            } catch (IOException | NumberFormatException | NoSuchElementException ex) {
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

        private void initProbMatrix(List<SmallCell> cellsParsed, Map<String, Double> probMatrix) {
            probMatrix = new HashMap<>();
            for (SmallCell nxtCl : cellsParsed) {
                for (SmallCell nxtNghb : cellsParsed) {
                    probMatrix.put(hashKey(nxtCl, nxtNghb), 0.0);
                }
            }
        }

        String hashKey(SmallCell a, SmallCell b) {
            // use id + coordinates due to collitions when hashing the string based only on IDs..
            return a.getID()
                    + "@" + a.getCoordinates().toSynopsisString()
                    + ":" + b.getID()
                    + "@" + b.getCoordinates().toSynopsisString();
        }
    }

}
