package sim.content;

import app.properties.Cost;
import app.properties.valid.Values;
import sim.ISimulationMember;
import sim.run.SimulationBaseRunner;
import sim.space.cell.CellRegistry;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public abstract class AbstractContent implements IContent, ISimulationMember {

    private final long _id;
    private final long _sizeBytes;
    private final SimulationBaseRunner<?> _sim;


    private final String _remoteCostType;

    protected double _costOfRmtTransfer;
    protected double _costOfMCWireless;
    protected double _costOfSCWireless;

    /**
     * Creates a new item. Note that the size loaded is expected to be in bytes.
     *
     * @param id
     * @param sim
     * @param sizeInBytes
     */
    protected AbstractContent(long id, SimulationBaseRunner<?> sim, long sizeInBytes) {

        _sizeBytes = sizeInBytes;
        _id = id;
        _sim = sim;

        _remoteCostType = getSimulation().getScenario().stringProperty(Cost.Transfer.TRANSFER_COST_ON_MISS__TYPE, false);

        double hopMean = getSimulation().getScenario().doubleProperty(Cost.Transfer.TRANSFER__PROPAGATION__HOP_COUNT__MEAN);
        double hopStdev = getSimulation().getScenario().doubleProperty(Cost.Transfer.TRANSFER__PROPAGATION__HOP_COUNT__STD);
        redefineRemoteTransferAndMCCost(hopMean, hopStdev); // call again if CDN exists after creating all documents

        if (_remoteCostType.equalsIgnoreCase(Values.MONETARY)) {

            _costOfSCWireless = 0.0;

            _costOfMCWireless = getSimulation().getScenario().doubleProperty(Cost.Transfer.MC__MDU);
        } else if (_remoteCostType.equalsIgnoreCase(Values.PROPAGATION_DELAY__PLUS__MC_WIRELESS)) {

            _costOfSCWireless = getSimulation().getScenario().doubleProperty(
                    Cost.Transfer.COST__TRANSFER__WIRELESS_HOP_COST__SC);

            _costOfMCWireless = getSimulation().getScenario().doubleProperty(
                    Cost.Transfer.COST__TRANSFER__WIRELESS_HOP_COST__MC);
        } else {
            throw new UnsupportedOperationException(
                    "Unknown value \"" + _remoteCostType + "\""
                    + " passed to property " + Cost.Transfer.TRANSFER_COST_ON_MISS__TYPE);
        }
    }

    public static int sumSize(AbstractContent... contents) {
        int sum = 0;
        for (AbstractContent nxt : contents) {
            sum += nxt.sizeInBytes();
        }
        return sum;
    }

    @Override
    public final long getID() {
        return _id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AbstractContent other = (AbstractContent) obj;
        if (this._id != other._id) {
            return false;
        }
        // Caution!
        // Do not 
        return this._sizeBytes == other._sizeBytes;
        
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + (int) (this._id ^ (this._id >>> 32));
//        hash = 41 * hash + (int) (this._sizeBytes ^ (this._sizeBytes >>> 32));
         //Caution!
        // don't do that.. have it the same between different sims, so as to share between sims
        // --> hash = 41 * hash + this._sim.hashCode();
        return hash;
    }

   

    @Override
    public final long sizeInBytes() {
        return _sizeBytes;
    }

    @Override
    public final double sizeInMBs() {
        return _sizeBytes / Math.pow(1024, 2);
    }
    @Override
    public final double sizeInChunks() {
        return (double)_sizeBytes / getSimulation().chunkSizeInBytes();
    }

    @Override
    public String toString() {
        StringBuilder strBld = new StringBuilder().
                append(getClass().getSimpleName()).
                append(": ");
        strBld.append("<id=").
                append(getID()).
                append("; size ").
                append(sizeInMBs()).
                append(" MB>");
//xxx uncomment the following. tmp commenting for debugging only
//        if (_remoteCostType.equalsIgnoreCase(Values.PROPAGATION_DELAY__PLUS__MC_WIRELESS)) {
//            try {
//                strBld.append("; Remote transfer+MC transfer cost=").append(costOfTransferMC_BH()).
//                        append("; BH+SC transfer cost=").append(costOfTransferBHandSC()).
//                        append("; Cached (SC only) cost=").append(costOfTransferSC());
//            } catch (Throwable ex) {
//                strBld.append("; Remote transfer+MC transfer cost=").append(costOfTransferMC_BH()).
//                        append("; BH+SC transfer cost=").append("ERROR").
//                        append("; Cached (SC only) cost=").append("ERROR");
//            }
//        }
        return strBld.toString();
    }

    @Override
    public final int simTime() {
        return _sim.simTime();
    }

    @Override
    public final String simTimeStr() {
        return "[" + simTime() + "]";
    }

    @Override
    public final int simID() {
        return _sim.getID();
    }

    @Override
    public final SimulationBaseRunner<?> getSimulation() {
        return _sim;
    }

    @Override
    public final CellRegistry simCellRegistry() {
        return _sim.getCellRegistry();
    }

    @Override
    public abstract ContentDocument referredContentDocument();

    @Override
    public final double gainOfTransferSCThroughBH() {
        return costOfTransferMC_BH() - costOfTransferSC_BH();
    }

    @Override
    public final double gainOfTransferSCCacheHit() {
        return costOfTransferMC_BH() - costOfTransferSCCacheHit();
    }

    @Override
    public final double costOfTransferSC_BH() {
        return _costOfSCWireless + _costOfRmtTransfer;
    }

    @Override
    public final double costOfTransferSCCacheHit() {
        return _costOfSCWireless;
    }

    /**
     *
     * @return
     */
    @Override
    public final double costOfTransferMC_BH() {
        return _costOfMCWireless + _costOfRmtTransfer;
    }

    public final void redefineRemoteTransferAndMCCost(double hopMean, double hopStdev) {
        if (_remoteCostType.equalsIgnoreCase(Values.MONETARY)) {
            _costOfRmtTransfer = 0;
            return;
        }
        if (_remoteCostType.equalsIgnoreCase(Values.PROPAGATION_DELAY__PLUS__MC_WIRELESS)) {
            double ratio = getSimulation().getRandomGenerator().getGaussian(
                    hopMean, hopStdev
            );
            _costOfRmtTransfer = ratio * getSimulation().getScenario()
                    .doubleProperty(Cost.Transfer.TRANSFER__HOP_COST);

            return;
        }

        throw new UnsupportedOperationException("Unkown value \"" + _remoteCostType + "\""
                + " passed to property " + Cost.Transfer.TRANSFER_COST_ON_MISS__TYPE);

    }



   

}
