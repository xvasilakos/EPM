package caching.incremental;

import caching.Utils;
import caching.base.AbstractCachingPolicy;
import java.util.Collection;
import sim.run.SimulationBaseRunner;
import sim.content.Chunk;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;
import sim.space.users.StationaryUser;
import sim.space.users.mobile.MobileUser;
import sim.space.util.DistanceComparator;

/**
 *
 * Check if functionality is correct.
 *
 * Extends sum.EPC1 by considering time to handoff as an inversive factor for
 * assessing cache decisions.
 *
 * @author xvas
 */
@Deprecated
public final class EMC_with_HandOffTime extends caching.incremental.EMC {

    private static EMC_with_HandOffTime singleton = new EMC_with_HandOffTime();

    /**
     * @return the singleton instance of this class according to its placement
     * in the hierarchy of AbstractMethod class descendants.
     */
    public static AbstractCachingPolicy instance() {
        return singleton;
    }

    protected EMC_with_HandOffTime() {
    }

  

    @Override
    public int cacheDecision(SimulationBaseRunner sim, CachingUser cu, Collection<Chunk> requestChunks, SmallCell hostSC, SmallCell sc2Cache)
            throws Throwable {

        double distance = DistanceComparator.euclidianDistance(cu, sc2Cache) - sc2Cache.getRadius();
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
            if (sc2Cache.isCached(this, nxtItem)) {
                continue;
            }

            double cachePrice = sc2Cache.cachePricePoll(this);
            double assessment = assess(cu, nxtItem, sc2Cache) / time2Handoff;

            if (assessment / nxtItem.sizeInMBs() >= cachePrice) {
                if (!Utils.isSpaceAvail(this, sc2Cache, nxtItem.sizeInBytes())) {//since no eviction policy supported
                    continue;
                }

                totalSizeCached += nxtItem.sizeInBytes();
                sc2Cache.cacheItemAttempt(cu, this, nxtItem);
            }

        }
        return totalSizeCached;
    }

}
