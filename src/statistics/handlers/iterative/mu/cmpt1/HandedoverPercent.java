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
public class HandedoverPercent extends BaseHandler implements IComputePercent{

   public static final String STATS__HANDLERS_package = HandedoverPercent.class.getPackage().getName();


   public HandedoverPercent() {
      super();
   }

   /**
    * Returns 1 if handed over to another cell in the previous simulation simTime ; otherwise 0. 
    * 
    * The reason why it refers
    * to the previous round is because the handoff state of a MU can be defined and updated only after updating
    * iterative statistics such as the current one (see simulation.run()).
    *
    * @param sim ignored
    * @param mu
    * @param cell ignored
    * @return 1 if handed over to another cell in the previous simulation simTime ; otherwise 0
    */
   @Override
   public final double computePercent(SimulationBaseRunner sim, MobileUser mu, AbstractCell cell) {
      return mu.getLastHandoffTime() == sim.simTime() ? 1.0 : 0;
   }

   @Override
   public String title() {
      return "%MUs_Handover";
   }
}
