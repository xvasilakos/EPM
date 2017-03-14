package caching.incremental;

import caching.Utils;
import caching.base.AbstractEPC;
import caching.base.AbstractCachingModel;
import caching.base.AbstractPricing;
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

    @Override
    public int cacheDecision(SimulationBaseRunner sim, CachingUser mu, Collection<Chunk> requestChunks, SmallCell hostSC, SmallCell targetSC) throws Throwable {
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
        return cacheDecision(this, sim, mu, requestChunks, hostSC, targetSC);
    }

    static int cacheDecision(IGainNoRplc _model, SimulationBaseRunner sim,
            CachingUser cu, Collection<Chunk> requestChunks, SmallCell hostSC, SmallCell targetSC) throws Throwable {

        AbstractPricing model = (AbstractPricing) _model;

        int totalSizeCached = 0;
        for (Chunk nxtChunk : requestChunks) {

            double cachePrice = targetSC.cachePrice(model);
            double assessment = -1.0;

            if (_model instanceof caching.incremental.EMC) {
                assessment = ((caching.incremental.EMC) _model).assess(cu, nxtChunk, hostSC);
            }
            if (_model instanceof caching.incremental.EPC) {
                assessment = ((caching.incremental.EPC) _model).assess(cu, nxtChunk, hostSC);
            }
            if (_model instanceof EPCP) {
                assessment = ((EPCP) _model).assess(cu, nxtChunk, hostSC);
            }
            if (_model instanceof PopOnly) {
                assessment = ((PopOnly) _model).assess(nxtChunk, hostSC);
            }
            if (assessment == -1.0) {
                throw new UnsupportedOperationException("Not supported for: " + _model.getClass().getCanonicalName());
            }

//never do that:
//            if (targetSC.isCached(model, nxtChunk)) {
//                targetSC.addCacher(cu, model, nxtChunk);
//                continue;
//            }

            if (assessment / nxtChunk.sizeInMBs() >= cachePrice) {
//                targetSC.cacheItem(cu, model, nxtChunk);
                if (targetSC.cacheItemAttempt(cu, model, nxtChunk) == Success) {
                    totalSizeCached += nxtChunk.sizeInBytes();
                    targetSC.cachePriceUpdt(model);
                }
            } 
//            else {
//                throw new RuntimeException(
//                        "policy_ instanceof caching.incremental.EMC" + (policy_ instanceof caching.incremental.EMC) + "\n"
//                        + "policy_ " + (policy_.getClass().getCanonicalName()) + "\n"
//                        + "assessment=" + assessment + "\n"
//                        + "nxtChunk.sizeInMBs()=" + nxtChunk.sizeInMBs() + "\n"
//                        + "assessment / nxtChunk.sizeInMBs()=" + (assessment / nxtChunk.sizeInMBs()) + "\n"
//                        + "cachePrice=" + cachePrice + "\n"
//                );
//            }

        }
        return totalSizeCached;
    }

}
