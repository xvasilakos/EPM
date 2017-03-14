package statistics.handlers.iterative.sc.cmpt4.no_policy;

import java.util.Set;
import sim.space.cell.smallcell.SmallCell;
import statistics.handlers.BaseHandler;
import statistics.handlers.ICompute4;

/**
 * @author Xenofon Vasilakos xvas@aueb.gr
 *
 * The total number of SC neighbors, i.e. neighbors with at least one outgoing
 * handoff from the given SC.
 */
public class NeighborsCount extends BaseHandler implements ICompute4<SmallCell> {

    /**
     * @param cell
     * @return
     */
    @Override
    public final double compute4(SmallCell cell) {
        Set<SmallCell> neighCells = cell.neighbors();
        return neighCells.size();
    }

    @Override
    public String title() {
        return "#Neighbs/SC";
    }
}
