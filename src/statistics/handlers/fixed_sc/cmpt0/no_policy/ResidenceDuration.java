package statistics.handlers.fixed_sc.cmpt0.no_policy;

import sim.space.cell.smallcell.SmallCell;
import statistics.StatisticException;
import statistics.handlers.ICompute0;

/**
 * @author Xenofon Vasilakos xvas@aueb.gr
 *
 */
public class ResidenceDuration extends
        statistics.handlers.iterative.sc.cmpt4.no_policy.ResidenceDuration 
        implements ICompute0 {

    private final SmallCell _monitorSC;
    
    public ResidenceDuration(SmallCell monitorSC) {
        super();
        _monitorSC = monitorSC;
    }

    @Override
    public String title() {
        return super.title()+ "["+_monitorSC.getID()+"]";
    }

    @Override
    public double compute0() throws StatisticException {
        return super.compute4(_monitorSC);
    }
   
}
