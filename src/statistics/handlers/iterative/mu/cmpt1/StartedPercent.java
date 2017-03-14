package statistics.handlers.iterative.mu.cmpt1;

import sim.run.SimulationBaseRunner;
import sim.space.cell.AbstractCell;
import sim.space.users.mobile.MobileUser;
import statistics.handlers.BaseHandler;
import statistics.handlers.IComputePercent;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class StartedPercent extends BaseHandler implements IComputePercent {
      public static final String STATS__HANDLERS_package = StartedPercent.class.getPackage().getName();


   public StartedPercent() {
      super();
   }

   /**
    *
    * @param sim ignored
    * @param mu
    * @param cell ignored
    * @return 1 if the MU has started roaming in the simulation; otherwise 0
    */
   @Override
   public final double computePercent(SimulationBaseRunner sim, MobileUser mu, AbstractCell cell)  {
      return mu.afterStartingRoaming() ? 1 : 0;
 }

   @Override
   public String title() {
      return "%MUs_Started";
   }
}
