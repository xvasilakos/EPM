package statistics.handlers.iterative.mu.cmpt1;

import sim.run.SimulationBaseRunner;
import sim.space.cell.AbstractCell;
import sim.space.users.mobile.MobileUser;
import statistics.handlers.BaseHandler;
import statistics.handlers.IComputePercent;

/**
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class MovedPercent extends BaseHandler implements IComputePercent {

   public MovedPercent() {
      super();
   }

   /**
    *
    * @param sim
    * @param mu
    * @param cell
    * @return 1 if moved in the current simulation simTime; otherwise 0
    */
   @Override
   public final double computePercent(SimulationBaseRunner sim, MobileUser mu, AbstractCell cell) {
      return sim.simTime() == mu.getLastTimeMoved() ? 1.0 : 0;
   }

   @Override
   public String title() {
      return "%MUs_Moved";
   }
}
