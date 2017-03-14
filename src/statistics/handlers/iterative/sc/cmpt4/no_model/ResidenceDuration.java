package statistics.handlers.iterative.sc.cmpt4.no_model;

import sim.space.cell.smallcell.SmallCell;
import statistics.handlers.BaseHandler;
import statistics.handlers.ICompute4;

/**
 * 
 * @author Xenofon Vasilakos <xvas@aueb.gr - mm.aueb.gr/~xvas>,
 * Mobile Multimedia Laboratory <mm.aueb.gr>,
 * Dept. of Informatics, School of Information Sciences & Technology,
 * Athens University of Economics and Business, Greece
 */
public class ResidenceDuration extends BaseHandler implements ICompute4<SmallCell> {

    /**
     * @param cell
     * @return
     */
    @Override
    public final double compute4(SmallCell cell) {
        double avgResidenceTime = cell.getSmoothedResidenceDuration();
        return avgResidenceTime;
    }

    @Override
    public String title() {
        return "SmoothedDurResidence/SC";
    }
}
