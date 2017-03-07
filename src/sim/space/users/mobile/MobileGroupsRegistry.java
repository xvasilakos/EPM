/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sim.space.users.mobile;

import app.properties.Space;
import exceptions.InconsistencyException;
import exceptions.InvalidOrUnsupportedException;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import sim.ISimulationMember;
import utils.ISynopsisString;
import sim.Scenario;
import sim.run.SimulationBaseRunner;
import sim.space.cell.CellRegistry;
import utils.CommonFunctions;

/**
 *
 * @author xvas
 */
public class MobileGroupsRegistry implements ISimulationMember, ISynopsisString {

    private final SimulationBaseRunner simulation;
    private final SortedMap<Integer, MobileGroup> groups;
    private final List<Integer> sizeGrp;

    private final int groupsNum;
    private final int totalNumOfMUs;
    private List<String> _initPosGrp;
    private List<String> _resetPosGrp;
    private List<Integer> roamStartGrp;

    private List<Double> velocityMeanGrp;
    private List<Double> velocityStdevGrp;
    private List<Integer> _residenceDelayInSCGrp;
    private List<Integer> _handoverDelayGrp;

    /**
     * A list of probabilities that form the probabilities matrix and flag which
     * denotes how the matrix should be interpreted.
     */
    private List<double[][]> _transProbsPerGrp;

    public SortedMap<Integer, MobileGroup> registeredGroups() {
        return this.groups;
    }

    public int size() {
        return registeredGroups().size();
    }

    public MobileGroupsRegistry(SimulationBaseRunner theSim) {
        this.simulation = theSim;

        //<editor-fold defaultstate="collapsed" desc="load from properties, report consistnecy issues">
        Scenario scenario = MobileGroupsRegistry.this.simulation.getScenario();
        try {
            // appart from sizes of each group, defines also the number of registeredGroups
            sizeGrp = scenario.listOfIntegersProperty(Space.MU__GROUP__SIZE);
            groupsNum = sizeGrp.size();

            initInitPosGrp(scenario);
            initResetPosGrp(scenario);
            initRoamStartGrp(scenario);
            initHandoffResidenceDelays(scenario);

            
            initVelocityStdevGrp(scenario);
            initVelocityMeanGrp(scenario);
            initTransProbsPerGrp(scenario);

        } catch (InvalidOrUnsupportedException ex) {
            throw new InconsistencyException("Needed property is undefined or wrongly spelt", ex);
        }
        //</editor-fold>

        int totalMUsNumSum = 0;
        this.groups = new TreeMap<>();
        for (int nxtGrpIdx = 0; nxtGrpIdx < groupsNum; nxtGrpIdx++) {
            int nxt_grp__ID = nxtGrpIdx + 1;

            int residenceDelayInSC = -1;
            residenceDelayInSC = residenceDelayInSCGrpFor(nxtGrpIdx);

  

            int handoverDelayInSC = -1;
            handoverDelayInSC = handoverDelayGrpFor(nxtGrpIdx);

        

            double meanVelocity = -1;
            meanVelocity = velocityMeanGrpFor(nxtGrpIdx);

            double stdevVelocity = -1;
            stdevVelocity = velocityStdevGrpFor(nxtGrpIdx);

            int startMUsRoaming = -1;
            startMUsRoaming = roamStartGrpFor(nxtGrpIdx);

            double[][] transitionProbs = null;
            transitionProbs = transProbsPerGrpFor(nxtGrpIdx);

            String initPos = initPosGrpFor(nxtGrpIdx);

            String resetPos = resetPosGrpFor(nxtGrpIdx);

            totalMUsNumSum += sizeGrp.get(nxtGrpIdx);

            MobileGroup nxtMUGrp = new MobileGroup(
                    simulation, nxt_grp__ID, sizeGrp.get(nxtGrpIdx), initPos, resetPos,
                    residenceDelayInSC,
                    handoverDelayInSC,
                    meanVelocity, stdevVelocity,
                    startMUsRoaming, transitionProbs
            );
            this.groups.put(nxtGrpIdx, nxtMUGrp);
        }//for
        this.totalNumOfMUs = totalMUsNumSum;
    }



    private String resetPosGrpFor(int nxtGrpIdx) {
        String resetPos;
        if (this._resetPosGrp.size() == 1) {//cross group value
            resetPos = this._resetPosGrp.get(0);
        } else {
            resetPos = this._resetPosGrp.get(nxtGrpIdx);
        }
        return resetPos;
    }

    private String initPosGrpFor(int nxtGrpIdx) {
        String initPos;
        if (this._initPosGrp.size() == 1) {//cross group value
            initPos = this._initPosGrp.get(0);
        } else {
            initPos = this._initPosGrp.get(nxtGrpIdx);
        }
        return initPos;
    }

    private double[][] transProbsPerGrpFor(int nxtGrpIdx) {
        double[][] transitionProbs;
        if (this._transProbsPerGrp.size() == 1) { //cross group value
            transitionProbs = this._transProbsPerGrp.get(0);
        } else {
            transitionProbs = this._transProbsPerGrp.get(nxtGrpIdx);
        }
        return transitionProbs;
    }

    private int roamStartGrpFor(int nxtGrpIdx) {
        int startMUsRoaming;
        if (this.roamStartGrp.size() == 1) {//cross group value
            startMUsRoaming = this.roamStartGrp.get(0);
        } else {
            startMUsRoaming = this.roamStartGrp.get(nxtGrpIdx);
        }
        return startMUsRoaming;
    }

    private double velocityStdevGrpFor(int nxtGrpIdx) {
        double stdevVelocity;
        if (this.velocityStdevGrp.size() == 1) {//cross group value
            stdevVelocity = this.velocityStdevGrp.get(0);
        } else {
            stdevVelocity = this.velocityStdevGrp.get(nxtGrpIdx);
        }
        return stdevVelocity;
    }

    private double velocityMeanGrpFor(int nxtGrpIdx) {
        double meanVelocity;
        if (this.velocityMeanGrp.size() == 1) {//cross group value
            meanVelocity = this.velocityMeanGrp.get(0);
        } else {
            meanVelocity = this.velocityMeanGrp.get(nxtGrpIdx);
        }
        return meanVelocity;
    }


    private int handoverDelayGrpFor(int nxtGrpIdx) {
        int handoverDelayInSC;
        if (this._handoverDelayGrp.size() == 1) {//cross group value
            handoverDelayInSC = this._handoverDelayGrp.get(0);
        } else {
            handoverDelayInSC = this._handoverDelayGrp.get(nxtGrpIdx);
        }
        return handoverDelayInSC;
    }

    private int residenceDelayInSCGrpFor(int nxtGrpIdx) {
        int residenceDelayInSC;
        if (this._residenceDelayInSCGrp.size() == 1) {//cross group value
            residenceDelayInSC = this._residenceDelayInSCGrp.get(0);
        } else {
            residenceDelayInSC = this._residenceDelayInSCGrp.get(nxtGrpIdx);
        }
        return residenceDelayInSC;
    }

    private void initTransProbsPerGrp(Scenario scenario) throws InvalidOrUnsupportedException {
        _transProbsPerGrp = scenario.parseMobileTransProbs();
        if (_transProbsPerGrp.size() != 1 && _transProbsPerGrp.size() != groupsNum) {
            throw new InvalidOrUnsupportedException(
                    "Transition probabilities " + Space.MU__TRANSITION_PROBABILITIES__MATRIX.name() + " has "
                    + _transProbsPerGrp.size() + " defined values for one simulation run. It  must have either one"
                    + " cross-group value or exactly " + groupsNum + ", i.e. one value per corresponding group of MUs");
        }
    }

    private void initVelocityMeanGrp(Scenario scenario) throws InvalidOrUnsupportedException {
        //<editor-fold defaultstate="collapsed" desc="velocityMeanGrp">
        velocityMeanGrp = scenario.listOfDoublesProperty(Space.MU__GROUP__VELOCITY__MEAN);
        if (velocityMeanGrp.size() != 1 && velocityMeanGrp.size() != groupsNum) {
            throw new InvalidOrUnsupportedException(
                    "Property " + Space.MU__GROUP__VELOCITY__MEAN
                    + " has " + velocityMeanGrp.size() + " values. It  must have either one cross-group value"
                    + " or exactly one value per corresponding group. Currently it has "
                    + velocityMeanGrp.size() + " while the number of groups is " + groupsNum);
        }
        //</editor-fold>
    }

    private void initVelocityStdevGrp(Scenario scenario) throws InvalidOrUnsupportedException {
        //<editor-fold defaultstate="collapsed" desc="velocityMax_perGrp">
        velocityStdevGrp = scenario.listOfDoublesProperty(Space.MU__GROUP__VELOCITY__STDEV);
        if (velocityStdevGrp.size() != 1 && velocityStdevGrp.size() != groupsNum) {
            throw new InvalidOrUnsupportedException(
                    "Property " + Space.MU__GROUP__VELOCITY__STDEV
                    + " has " + velocityStdevGrp.size() + " values. It  must have either one cross-group value"
                    + " or exactly one value per corresponding group. Currently it has "
                    + velocityStdevGrp.size() + " while the number of groups is " + groupsNum);
        }
        //</editor-fold>
    }

    private void initRoamStartGrp(Scenario scenario) throws InvalidOrUnsupportedException {
        roamStartGrp = scenario.listOfIntegersProperty(Space.MU__INIT__ROAM_START);
        if (roamStartGrp.size() != 1 && roamStartGrp.size() != groupsNum) {
            throw new InvalidOrUnsupportedException(
                    "Property " + Space.MU__INIT__ROAM_START + " has "
                    + roamStartGrp.size() + " values. It  must have either one cross-group value or exactly "
                    + "one value per corresponding group. Currently it has " + roamStartGrp.size()
                    + " while the number of groups is " + groupsNum);
        }
    }

    private void initResetPosGrp(Scenario scenario) throws InvalidOrUnsupportedException {
        _resetPosGrp = scenario.listOfStringsProperty(Space.MU__GROUP__RESET__POS, false);
        if (_resetPosGrp.size() != 1 && _resetPosGrp.size() != groupsNum) {
            throw new InvalidOrUnsupportedException(
                    "Property " + Space.MU__GROUP__RESET__POS
                    + " has " + _resetPosGrp.size() + " values. It  must have either one cross-group value or exactly"
                    + " one value for each corresponding group. The number of groups is " + groupsNum);
        }
    }

    private void initInitPosGrp(Scenario scenario) throws InvalidOrUnsupportedException {
        _initPosGrp = scenario.listOfStringsProperty(Space.MU__GROUP__INIT__POS, false);
        if (_initPosGrp.size() != 1 && _initPosGrp.size() != groupsNum) {
            throw new InvalidOrUnsupportedException(
                    "Property " + Space.MU__GROUP__INIT__POS
                    + " has " + _initPosGrp.size() + " values. It  must have either one cross-group value or exactly"
                    + " one value for each corresponding group. The number of groups is " + groupsNum);
        }
    }

    private void initHandoffResidenceDelays(Scenario scenario) throws InvalidOrUnsupportedException {
        _residenceDelayInSCGrp = scenario.listOfIntegersProperty(Space.MU__GROUP__RESIDENCE_DELAY);
        if (_residenceDelayInSCGrp.size() != 1 && _residenceDelayInSCGrp.size() != groupsNum) {
            throw new InvalidOrUnsupportedException(
                    "Property " + Space.MU__GROUP__RESIDENCE_DELAY
                    + " has " + _residenceDelayInSCGrp.size() + " values. It  must have either one cross-group value"
                    + " or exactly one value per corresponding group. Currently it has "
                    + _residenceDelayInSCGrp.size() + " while the number of groups is " + groupsNum);
        }

        _handoverDelayGrp = scenario.listOfIntegersProperty(Space.MU__GROUP__HANDOVER_DELAY);
        if (_handoverDelayGrp.size() != 1 && _handoverDelayGrp.size() != groupsNum) {
            throw new InvalidOrUnsupportedException(
                    "Property " + Space.MU__GROUP__HANDOVER_DELAY
                    + " has " + _handoverDelayGrp.size() + " values. It  must have either one cross-group value"
                    + " or exactly one value per corresponding group. Currently it has "
                    + _handoverDelayGrp.size() + " while the number of groups is " + groupsNum);
        }

    }

    /**
     * @param id
     * @return
     * @throws InvalidOrUnsupportedException if the group does not exist. Note
     * that group IDs start from index value 1.
     */
    public MobileGroup getGroup(int id) throws InvalidOrUnsupportedException {
        MobileGroup ug = groups.get(id);
        if (ug == null) {
            throw new InvalidOrUnsupportedException(
                    "No such group id. IDs for groups span from 1 to N, where N is the maximum number of groups defined for "
                    + "this simulation, i.e. "
                    + simulation.getScenario().listOfIntegersProperty(Space.MU__GROUP__SIZE).size());
        }
        return ug;
    }

    @Override
    public final int simTime() {
        return getSim().simTime();
    }

    @Override
    public String simTimeStr() {
        return "[" + simTime() + "]";
    }

    @Override
    public final int simID() {
        return getSim().getID();
    }

    @Override
    public final SimulationBaseRunner getSim() {
        return simulation;
    }

    @Override
    public final CellRegistry simCellRegistry() {
        return getSim().getCellRegistry();
    }

    /**
     * Generates a unique universal ID for a MU__CLASS such that the 1st MU__CLASS member of
 the 1st group gets ID 1, the 1st MU__CLASS of the 2nd group gets ID 2, the 1st
 MU__CLASS of the 3rd group gets id 3 and so forth.
     *
     * Mathematically, the former is described by formula: (k-1) *
     * #registeredGroups + grpID.
     *
     * CAUTION k must be greater than 0 and grpID must be a non negative value.
     *
     * @param grpID the ID of the group
     * @param k the sequence number of the MU__CLASS with respect to its group, e.g. 5
 for the 5th MU__CLASS of the given group.
     * @return
     */
    public int generateMobileID(int grpID, int k) {
        if (k < 1) {
            throw new RuntimeException("The sequence number k of the MU must be greater than zero.  "
                    + "Value passed: k=" + k);
        }
        if (grpID < 0) {
            throw new RuntimeException("The group id must be non negative.  Value passed grpID=" + grpID);
        }
        return grpID + (k - 1) * groupsNum;
    }

    /**
     * @return the totalNumOfMUs
     */
    public int getTotalNumOfMUs() {
        return totalNumOfMUs;
    }

    @Override
    /**
     * @return true iff same hashID.
     */
    public boolean equals(Object b) {
        if (b == null) {
            return false;
        }

        if (!(b instanceof MobileGroupsRegistry)) {
            return false;
        }

        MobileGroupsRegistry rg = (MobileGroupsRegistry) b;
        return rg.getSim().equals(getSim());
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + Objects.hashCode(this.simulation);
        hash = 97 * hash + Objects.hashCode(this.groups);
        return hash;
    }

    @Override
    public String toSynopsisString() {
        return simulation.toString() + "; "
                + CommonFunctions.toString(groups) + "; "
                + " #groupos: " + groupsNum + "; "
                + " # total num of mobiles: " + totalNumOfMUs;
    }

    @Override
    public String toString() {
        return toSynopsisString();
    }

}
