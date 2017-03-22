package caching.incremental;

import caching.Utils;
import caching.base.AbstractEPC;
import caching.base.AbstractCachingModel;
import caching.base.AbstractPricingModel;
import caching.interfaces.incremental.IIncremental;
import caching.interfaces.rplc.IGainNoRplc;
import java.util.Collection;
import sim.run.SimulationBaseRunner;
import sim.content.Chunk;
import static sim.space.cell.smallcell.BufferBase.BufferAllocationStatus.Success;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;

/**
 * Efficient Proactive Caching implementation with Cache decisions cancellation
 * supported and no support for any cache replacement policies. Note that if any
 * of the mobile requesters tries to cancel a request for an item, then the item
 * is evicted, which differentiates this implementation from Incremental2.
 *
 * @author xvas
 */
public class EPC extends AbstractEPC implements IIncremental, IGainNoRplc {

    private static EPC singleton = new EPC();

    /**
     * @return the singleton instance of this class according to its placement
     * in the hierarchy of AbstractMethod class descendants.
     */
    public static AbstractCachingModel instance() {
        return singleton;
    }

    protected EPC() {
    }

    @Override
    public String nickName() {
        return "EPC";
    }

//    @Override
//    public int cacheDecision(SimulationBaseRunner sim, CachingUser mu, Collection<Chunk> requestChunks, SmallCell hostSC, SmallCell targetSC) throws Throwable {
//        int totalSizeCached = 0;
//        for (Chunk nxtChunk : requestChunks) {
//
//            double cachePrice = targetSC.cachePrice(this);
//            double assessment = assess(mu, nxtChunk, targetSC);
//
////never do that:
////            if (targetSC.isCached(this, nxtItem)) {
////                targetSC.addCacher(mu, this, nxtItem);
////                continue;
////            }
//            if (targetSC.isCached(this, nxtChunk) || !Utils.isSpaceAvail(this, targetSC, nxtChunk.sizeInBytes())) {
//                continue; //since no eviction model supported
//            }
//
//            if (assessment / nxtChunk.sizeInMBs() >= cachePrice) {
//                totalSizeCached += nxtChunk.sizeInBytes();
//                targetSC.cacheItem(mu, this, nxtChunk);
//                targetSC.cachePriceUpdt(this);
//            }
//
//        }
//        return totalSizeCached;
//    }
    @Override
    public int cacheDecision(SimulationBaseRunner sim,
            CachingUser cu, Collection<Chunk> requestChunks, SmallCell hostSC, SmallCell targetSC) throws Throwable {

        int totalSizeCached = 0;
        for (Chunk nxtChunk : requestChunks) {

//never do that:
//            if (targetSC.isCached(model, nxtChunk)) {
//                targetSC.addCacher(cu, model, nxtChunk);
//                continue;
//            }
            double cachePrice = targetSC.cachePrice(this);
            double assessment = assess(cu, nxtChunk, targetSC);


            if (assessment / nxtChunk.sizeInMBs() >= cachePrice) {
//                targetSC.cacheItem(cu, model, nxtChunk);
                if (targetSC.cacheItemAttemptPriceUpdate(cu, this, nxtChunk) == Success) {
                    totalSizeCached += nxtChunk.sizeInBytes();
                }
            }
        }
        return totalSizeCached;
    }
    
    
    
    /**
     * @param chnk the chunk to be assessed
     * @param cu the requesting/caching user
     * @param sc the small cell for which the chunks are assessed
     * @return the expected gain weighted by the mobile transition probability
     */
    @Override
    public double assess(CachingUser cu, Chunk chnk, SmallCell sc) {
        return Utils.assessEPC(cu, chnk, sc);
    }

}
