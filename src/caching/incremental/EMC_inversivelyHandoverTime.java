package caching.incremental;

import caching.Utils;
import caching.base.AbstractCachingModel;
import java.util.Collection;
import sim.run.SimulationBaseRunner;
import sim.content.Chunk;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;
import sim.space.users.StationaryUser;
import sim.space.users.mobile.MobileUser;
import sim.space.util.DistanceComparator;

/**
 * Cache decisions are inversivly proportional to the expected time until
 * handover completion.
 *
 * @author Xenofon Vasilakos ({@literal xvas@aueb.gr} - mm.aueb.gr/~xvas),
 * Mobile Multimedia Laboratory (mm.aueb.gr), Dept. of Informatics, School of
 * Information {@literal Sciences & Technology}, Athens University of Economics
 * and Business, Greece
 * @deprecated
 */
//TODO Check if functionality is correct.
@Deprecated
public final class EMC_inversivelyHandoverTime extends caching.incremental.EMC {

    private static EMC_inversivelyHandoverTime singleton = new EMC_inversivelyHandoverTime();

    /**
     * @return the singleton instance of this class according to its placement
     * in the hierarchy of AbstractMethod class descendants.
     */
    public static AbstractCachingModel instance() {
        return singleton;
    }

    protected EMC_inversivelyHandoverTime() {
    }

    @Override
    public int cacheDecision(SimulationBaseRunner sim, CachingUser cu, Collection<Chunk> requestChunks, SmallCell hostSC, SmallCell targetSC)
            throws Throwable {

        double distance = DistanceComparator.euclidianDistance(cu, targetSC) - targetSC.getRadius();
        double velocity = -1;
        if (cu instanceof MobileUser) {
            velocity = ((MobileUser) cu).getVelocity();
        } else if (cu instanceof StationaryUser) {
            velocity = 0;
        } else {
            throw new UnsupportedOperationException();
        }

        double time2Handoff = distance / velocity; // time to handoff

        int totalSizeCached = 0;
        for (Chunk nxtItem : requestChunks) {
            if (targetSC.isCached(this, nxtItem)) {
                continue;
            }

            double cachePrice = targetSC.cachePrice(this);
            double assessment = assess(cu, nxtItem, targetSC) / time2Handoff;

            if (assessment / nxtItem.sizeInMBs() >= cachePrice) {
                if (!Utils.isSpaceAvail(this, targetSC, nxtItem.sizeInBytes())) {//since no eviction policy supported
                    continue;
                }

                totalSizeCached += nxtItem.sizeInBytes();
                targetSC.cacheItemAttempt(cu, this, nxtItem);
            }

        }
        return totalSizeCached;
    }

}
