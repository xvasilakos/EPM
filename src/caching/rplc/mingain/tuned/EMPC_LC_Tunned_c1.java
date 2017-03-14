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
 * interval "b" of the small cell.
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public final class EMPC_LC_Tunned_c1 extends AbstractEMPC_LC_Tunned {

    private static final AbstractCachingPolicy singelton = new EMPC_LC_Tunned_c1();

    public static AbstractCachingPolicy instance() {
        return singelton;
    }

    EMPC_LC_Tunned_c1() {
    }

    /**
     * Uses no replacement interval "b"
     *
     * @param dmdRegInfo
     * @param sc
     * @return
     */
    @Override
    protected boolean checkRplcAbort(PCDemand.RegistrationInfo dmdRegInfo, SmallCell sc) {
        Set<CachingUser> cus;

        cus = dmdRegInfo.cachingUsers();
        for (CachingUser nxtUser : cus) {
            double distance = -sc.getRadius() + DistanceComparator.euclidianDistance(
                    nxtUser, sc);
            double velocity = -1;
            if (nxtUser instanceof StationaryUser) {
                 velocity = 0;
            } else if (nxtUser instanceof MobileUser) {
                velocity = ((MobileUser)nxtUser).getVelocity();
            } else {
                throw new UnsupportedOperationException();
            }

            double time = distance / velocity; // time to handoff
            if (time < sc.getEPCLCnoRplcState().getC1()) {
                return true; // check the next item
            }
        }
        return false;
    }

}
