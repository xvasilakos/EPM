package statistics.handlers.iterative.sc.cmpt4.no_model;

import sim.space.cell.smallcell.SmallCell;
import statistics.handlers.BaseHandler;
import statistics.handlers.ICompute4;

/**
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class HandoverCount extends BaseHandler implements ICompute4<SmallCell> {


    /**
     * @param cell
     * @return
     */
    @Override
    public final double compute4(SmallCell cell) {
        double count = cell.getSmoothedHandoversCount();
        return count;
    }

    @Override
    public String title() {
        return "#Handovers[Smoothed]/SC";
    }
}
