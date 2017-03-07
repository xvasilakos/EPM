package statistics.handlers;

import sim.run.SimulationBaseRunner;
import sim.space.cell.AbstractCell;
import sim.space.users.mobile.MobileUser;

/**
 * Requires the SimulationBaseRunner , the MobileUser mu and the AbstractCell to computePercent statistics.
 * @author xvas
 */
public interface IComputePercent extends statistics.handlers.ICompute{
   
   public abstract double computePercent(SimulationBaseRunner sim, MobileUser mu, AbstractCell cell) throws statistics.StatisticException;
}
