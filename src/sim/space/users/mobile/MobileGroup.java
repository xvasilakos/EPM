package sim.space.users.mobile;

import sim.space.users.*;
import sim.run.SimulationBaseRunner;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class MobileGroup extends UserGroup {

    private final String _howToResetPos;
    private final int _residenceDelayInSC;
    private final int _handoverDelayInSC;
    private final double _velocityMean;
    private final double _velocityStdev;
    /**
     * MUs that start roaming with each simulation clock tick.
     */
    private final int musStartRoaming;
    private final double[][] transitionProbabilities;
    /**
     * The number of subset members as defined by the percentages in the first
     * column of transitionProbabilities.
     */
    private final int[] subsetMembers;

    public MobileGroup(
            SimulationBaseRunner sim, int idNum, int musNum, String posAtStart, String posAtReset, int residenceDelayInSC, int handoffDelayInSC, double meanVelocity, double stdevVelocity, int _roam_start, double[][] _transProbs) {

        super(sim, idNum, musNum, posAtStart);

        this._howToResetPos = posAtReset;

        this._residenceDelayInSC = residenceDelayInSC;
        this._handoverDelayInSC = handoffDelayInSC;

        this._velocityMean = meanVelocity;
        this._velocityStdev = stdevVelocity;

        musStartRoaming = _roam_start;
        this.transitionProbabilities = new double[_transProbs.length][_transProbs[0].length];
        System.arraycopy(_transProbs, 0, transitionProbabilities, 0, _transProbs.length);

        subsetMembers = new int[transitionProbabilities.length];
        /*
       * Let the subset members of the last subset be the remainder MUs. This
       * is useful for case that percentages do not match well with the nmber of MUs, e.g. 20% and 100% combined with 11 MUs in total
         */
        int sum = 0;
        for (int i = 0; i < _transProbs.length - 1; i++) {
            double percent = _transProbs[i][0];
            sum += subsetMembers[i] = (int) (percent * size);
        }
        subsetMembers[subsetMembers.length - 1] = size - sum;// last subset
    }

    /**
     * @return the position that MUs have after being reset to the area such as
     * south west, north etc..
     */
    public String getHowToResetPos() {
        return _howToResetPos;
    }

    /**
     * Computes the simulation start simTime for a particular MU based on its
     * sequential ID and simulation properties file.
     *
     * param muID the ID of the user for which the simulation start simTime is
     * computed
     * @return Computes the simulation start simTime for a particular MU based
     * on its ID.
     */
    public int startRoamingTime(int muID) {
        if (musStartRoaming < 1) {
            return 0;
        }
        return (int) (muID / musStartRoaming);
    }

    /**
     * The transition probabilities of this group's MUs.
     *
     * @return the transition probabilities of this MU after its group.
     */
    public double[][] getTransitionProbabilities() {
        return transitionProbabilities;
    }

    /**
     * @return the meanVelocity
     */
    public double getVelocityMean() {
        return _velocityMean;
    }

    /**
     * @return the stdevVelocity
     */
    public double getVelocityStdev() {
        return _velocityStdev;
    }

    public double gaussianVelocity() {
        return _simulation.getRandomGenerator().getGaussian(_velocityMean, _velocityStdev);
    }

    /**
     * Decides which subset of MUs this i_th MU member is, and returns the
     * transition probabilities matrix.
     *
     * param i_thMU
     * @return
     */
    public double[] transProbsForMember(int i_thMU) {
        int j = 0;
        int sum = 0;
        for (; j < subsetMembers.length; j++) {
            sum += subsetMembers[j];
            if (i_thMU <= sum) {
                double[] probsMatrix = new double[9];
                // start from 1, not zero, to skip the subset f MUs percentage

                for (int i = 1; i < transitionProbabilities[j].length; i++) {
                    probsMatrix[i - 1] = transitionProbabilities[j][i];
                }

                return probsMatrix;
            }
        }
        // sth is wrong if not returned above
        throw new RuntimeException("Method failed for  i_thMU=" + i_thMU);
    }

    /**
     * @return the _residenceDelayInSC
     */
    public int getResidenceDelayInSC() {
        return _residenceDelayInSC;
    }

    /**
     * @return the _handoverDelayInSC
     */
    public int getHandoverDelayInSC() {
        return _handoverDelayInSC;
    }

 
  

}
