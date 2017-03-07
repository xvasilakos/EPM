package caching.incremental;

import caching.Utils;
import caching.base.AbstractCachingPolicy;
import caching.base.AbstractEPCPop;
import caching.base.IEMPC;
import caching.interfaces.incremental.IIncrementalAggregate;
import java.util.Collection;
import sim.run.SimulationBaseRunner;
import sim.content.Chunk;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;

/**
 * Efficient Proactive Caching and LeGaCy POPularity implementation with Cache
 * decisions cancelation (CNC) supported and no support for any cache
 * replacement policies.
 *
 * Note that caching decisions use the weighted gain assessment defined in
 * AbstractAvgProb, i.e. they use the average transition probability of mobiles
 * requesting for each item requested.
 *
 * @author xvas
 */
public final class EMPC extends AbstractEPCPop implements IIncrementalAggregate, IEMPC {

    private static EMPC singleton = new EMPC();

    /**
     * @return the singleton instance of this class according to its placement
     * in the hierarchy of AbstractMethod class descendants.
     */
    public static AbstractCachingPolicy instance() {
        return singleton;
    }

    EMPC() {
    }

    @Override
    public String nickName() {
        return "EMPC";
    }

    @Override
    public int cacheDecision(
            SimulationBaseRunner sim, CachingUser mu, Collection<Chunk> requestChunks, SmallCell hostSC, SmallCell targetSC) throws Throwable {

        int totalSizeCached = 0;

        for (Chunk nxtItem : requestChunks) {

            if (targetSC.isCached(this, nxtItem)) {
//never do that neighbSC.addCacher(mu, this, nxtItem);
                continue;
            }

            if (!Utils.isSpaceAvail(this, targetSC, nxtItem.sizeInBytes())) { //since no replacement policy supported
                continue;//cannot add this item
            }

            double cachePrice = targetSC.cachePricePoll(this);
            if (assess(nxtItem, targetSC) / nxtItem.sizeInMBs() >= cachePrice) {
                totalSizeCached += nxtItem.sizeInBytes();
//                neighbSC.cacheItemAttempt(mu, this, nxtItem);
                targetSC.cacheItem(mu, this, nxtItem);
                targetSC.cachePriceUpdt(this);
            }
        }
        return totalSizeCached;
    }

    @Override
    public double assess(Chunk item, SmallCell sc) throws Throwable {
        return Utils.assessEMPC(item, sc, this);
    }

}
