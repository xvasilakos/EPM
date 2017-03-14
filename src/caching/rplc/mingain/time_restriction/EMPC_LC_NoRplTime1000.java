package caching.rplc.mingain.time_restriction;

import caching.base.AbstractCachingPolicy;
import sim.space.cell.demand_registry.PCDemand;
import sim.space.cell.smallcell.SmallCell;

/**
 * Same as EPCPop, only (practically) without replacements due to the high 
 * time threashold set (1000).
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public final class EMPC_LC_NoRplTime1000 extends EMPC_LC_NoRplTime01 {

   private static final AbstractCachingPolicy singelton = new EMPC_LC_NoRplTime1000();

   public static AbstractCachingPolicy instance() {
      return singelton;
   }

   @Override
   protected boolean checkAbortRplc(PCDemand.RegistrationInfo dmdRegInfo, SmallCell sc) {
      return checkAbortRplc(dmdRegInfo, sc, 1000/* minimum time to handoff 
       that replacement is allowed*/);
   }

}
