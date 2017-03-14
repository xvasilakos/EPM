package caching.incremental;

import caching.base.AbstractEMPC;
import caching.base.AbstractCachingModel;
import caching.interfaces.incremental.IIncremental;
import caching.Utils;
import java.util.Collection;
import sim.run.SimulationBaseRunner;
import sim.content.Chunk;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;

/**
 * EPC with content popularity extentions.
 *
 * @author xvas
 */
public final class EPCP extends AbstractEMPC implements IIncremental {

    private static final EPCP singleton = new EPCP();

    /**
     * @return the singleton instance of this class according to its placement
     * in the hierarchy of AbstractMethod class descendants.
     */
    public static AbstractCachingModel instance() {
        return singleton;
    }

    EPCP() {
    }

    @Override
    public String nickName() {
        return "EPC+Pop";
    }

    @Override
    public int cacheDecision(
            SimulationBaseRunner sim, CachingUser cu, Collection<Chunk> requestChunks, SmallCell hostSC, SmallCell sc) throws Throwable {
        return EPC.cacheDecision(this, sim, cu, requestChunks, hostSC, hostSC);
    }

//    @Override
//    public int cacheDecision(
//            SimulationBaseRunner sim, CachingUser cu, Collection<Chunk> requestChunks, SmallCell hostSC, SmallCell sc) throws Throwable {
//
//        int totalSizeCached = 0;
//        for (DocumentRequest nxtRequest : cu.getRequests()) 
//        for (Chunk nxtReqChunk : nxtRequest.referredContentDocument().chunks()){
//
//            if (sc.isCachedBy(cu, this, nxtReqChunk)) {
//                continue;
//            }
//
//            double cachePrice = sc.cachePricePoll(false, nxtReqChunk, this);
//            if (assess(cu, nxtReqChunk, sc) / nxtReqChunk.sizeInMBs() >= cachePrice) {
//
//                if (sc.isCached(this, nxtReqChunk)) {
//                    sc.addCacher(cu, this, nxtReqChunk);
//                    continue;
//                }
//
//                if (!Utils.isSpaceAvail(this, sc, nxtReqChunk.sizeInBytes())) { //since no replacement policy supported
//                    continue;//cannot add this item
//                }
//
//                totalSizeCached += nxtReqChunk.sizeInBytes();
//                sc.cacheItemAttempt(cu, this, nxtReqChunk);
//            }
//        }
//        return totalSizeCached;
//    }
    @Override
    public double assess(CachingUser mu, Chunk item, SmallCell sc) throws Throwable {
        return Utils.assessEPCWithPop(mu, item, sc, this);
    }

}
