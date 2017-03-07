package caching.incremental;

import caching.base.AbstractEPCPop;
import caching.base.AbstractCachingPolicy;
import caching.interfaces.incremental.IIncremental;
import caching.Utils;
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
 * Note that unlike CncAvgProb, caching decisions use a weighted gain assessment
 * based only on the transition probability of the requesting mobile user.
 *
 * @author xvas
 */
public final class EPC_with_Pop extends AbstractEPCPop implements IIncremental {

    private static final EPC_with_Pop singleton = new EPC_with_Pop();

    /**
     * @return the singleton instance of this class according to its placement
     * in the hierarchy of AbstractMethod class descendants.
     */
    public static AbstractCachingPolicy instance() {
        return singleton;
    }

    EPC_with_Pop() {
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
        return Utils.assessEPC_with_Pop(mu, item, sc, this);
    }

}
