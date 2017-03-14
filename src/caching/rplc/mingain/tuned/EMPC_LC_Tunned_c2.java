package caching.rplc.mingain.tuned;

import caching.base.AbstractCachingPolicy;
import java.util.Set;
import sim.space.cell.demand_registry.PCDemand;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;
import sim.space.users.StationaryUser;
import sim.space.users.mobile.MobileUser;
import sim.space.util.DistanceComparator;

/**
 * Same as EPCPop, only items do not get replaced when at least one of the
 * requesting mobiles gets close enough to the small cell, based on time
 * interval "c" of the small cell.
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public final class EMPC_LC_Tunned_c2 extends AbstractEMPC_LC_Tunned {

    private static final AbstractCachingPolicy singelton = new EMPC_LC_Tunned_c2();

    public static AbstractCachingPolicy instance() {
        return singelton;
    }

    EMPC_LC_Tunned_c2() {
    }

    /**
     * Uses no replacement interval "c"
     *
     * @param dmdRegInfo
     * @param sc
     * @return
     */
    @Override
    protected boolean checkRplcAbort(PCDemand.RegistrationInfo dmdRegInfo, SmallCell sc) {
        Set<CachingUser> cus;

        cus = dmdRegInfo.cachingUsers();
        for (CachingUser nxtCU : cus) {
            double distance = -sc.getRadius() + DistanceComparator.euclidianDistance(
                    nxtCU, sc);
            double velocity = -1;
            if (nxtCU instanceof MobileUser) {
                velocity = ((MobileUser) nxtCU).getVelocity();
            } else if (nxtCU instanceof StationaryUser) {
                velocity = 0;
            } else {
                throw new UnsupportedOperationException();
            }
            double time = distance / velocity; // time to handoff
            if (time < sc.getEPCLCnoRplcState().getC2()) {
                return true; // check the next item
            }
        }
        return false;
    }

}
