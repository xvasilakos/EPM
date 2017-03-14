package caching.rplc.mingain.time_restriction;

import caching.base.AbstractCachingPolicy;
import sim.space.cell.demand_registry.PCDemand;
import sim.space.cell.smallcell.SmallCell;

/**
 * Same as EPCPop, only items do not get replaced when at least one of the requesting
 * mobiles gets close enough to the small cell.
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public final class EMPC_LC_NoRplTime03 extends EMPC_LC_NoRplTime01 {

   private static final AbstractCachingPolicy singelton = new EMPC_LC_NoRplTime03();

   public static AbstractCachingPolicy instance() {
      return singelton;
   }

   EMPC_LC_NoRplTime03() {
   }

   @Override
   protected boolean checkAbortRplc(PCDemand.RegistrationInfo dmdRegInfo, SmallCell sc) {
      return checkAbortRplc(dmdRegInfo, sc, 3/* minimum time to handoff 
       that replacement is allowed*/);
   }

}
