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
public class DisconnectedPercent extends BaseHandler implements IComputePercent{

   public static final String STATS__HANDLERS_package = DisconnectedPercent.class.getPackage().getName();

   public DisconnectedPercent() {
      super();
   }


   @Override
   public final double computePercent(SimulationBaseRunner sim, MobileUser mu, AbstractCell cell) {
      return mu.getCurrentlyConnectedSC() == null ? 1.0 : 0;
   }

   @Override
   public String title() {
      return "%MUs_disconnected";
   }
}
