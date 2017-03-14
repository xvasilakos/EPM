package statistics.handlers.fixed_sc.cmpt0;

import caching.base.AbstractCachingPolicy;
import sim.space.cell.smallcell.SmallCell;
import statistics.StatisticException;
import statistics.handlers.ICompute0;

/**
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class BuffUtil extends statistics.handlers.iterative.sc.cmpt4.BuffUtil implements ICompute0{

    private final SmallCell _monitorSC;
    
    public BuffUtil(AbstractCachingPolicy cachingMethodUsed,  SmallCell monitorSC) {
        super(cachingMethodUsed);
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
