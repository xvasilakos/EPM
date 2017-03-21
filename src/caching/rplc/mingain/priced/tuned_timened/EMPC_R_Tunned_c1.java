package caching.rplc.mingain.priced.tuned_timened;

import caching.base.AbstractCachingModel;
import java.util.Set;
import sim.space.cell.demand_registry.PCDemand;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;
import sim.space.users.StationaryUser;
import sim.space.users.mobile.MobileUser;
import sim.space.util.DistanceComparator;

/**
 * Used in the tuning process of the time threshold applied by
 * #AbstractEMPC_R_Tunned.
 * 
 * Note: used in the golden ratio algorithm as parameter c1.
 *
 * @author Xenofon Vasilakos (xvas{@literal @}aueb.gr - mm.aueb.gr/~xvas),
 * Mobile Multimedia Laboratory (mm.aueb.gr),
 * Dept. of Informatics, School of Information {@literal Sciences & Technology},
 * Athens University of Economics and Business, Greece
 */
public final class EMPC_R_Tunned_c1 extends AbstractEMPC_R_Tunned {

    private static final AbstractCachingModel singelton = new EMPC_R_Tunned_c1();

    public static AbstractCachingModel instance() {
        return singelton;
    }

    EMPC_R_Tunned_c1() {
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
                velocity = ((MobileUser)nxtUser).getSpeed();
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
