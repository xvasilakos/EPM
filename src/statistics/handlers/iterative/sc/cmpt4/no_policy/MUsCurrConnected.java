package statistics.handlers.iterative.sc.cmpt4.no_policy;

import sim.space.cell.smallcell.SmallCell;
import statistics.handlers.BaseHandler;
import statistics.handlers.ICompute4;

/**
 * @author Xenofon Vasilakos xvas@aueb.gr
 *
 * The total number of SC neighbors, i.e. neighbors with at least one outgoing
 * handoff from the given SC.
 */
public class MUsCurrConnected extends BaseHandler implements ICompute4<SmallCell> {

    /**
     * @param cell
     * @return
     */
    @Override
    public final double compute4(SmallCell cell) {
        return cell.getConnectedMUs().size();
    }

    @Override
    public String title() {
        return "#MUs_Conn/SC";
    }
}
