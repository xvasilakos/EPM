package statistics.handlers.iterative.sc.cmpt4.no_policy;

import sim.space.cell.smallcell.SmallCell;
import statistics.handlers.BaseHandler;
import statistics.handlers.ICompute4;

/**
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class HandoverDuration extends BaseHandler implements ICompute4<SmallCell> {


    /**
     * @param cell
     * @return
     */
    @Override
    public final double compute4(SmallCell cell) {
        double avgHandoverTime = cell.getSmoothedHandoverDuration();
        return avgHandoverTime;
    }

    @Override
    public String title() {
        return "SmotthedDurHandover/SC";
    }
}
