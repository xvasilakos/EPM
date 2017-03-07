package caching.incremental;

import caching.Utils;
import caching.base.AbstractPop;
import caching.interfaces.incremental.IIncrementalAggregate;
import caching.interfaces.rplc.IGainNoRplc;
import java.util.Collection;
import sim.run.SimulationBaseRunner;
import sim.content.Chunk;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;

/**
 * Proactive Caching using POPularity with Cache decisions cancelation (CNC)
 * supported and no support for any cache replacement policies.
 *
 *
 * @author xvas
 */
public final class PopOnly extends AbstractPop implements IIncrementalAggregate, IGainNoRplc {

    private static final PopOnly singleton = new PopOnly();

    public static PopOnly instance() {
        return singleton;
    }

    PopOnly() {
    }

    @Override
    public String nickName() {
        return getClass().getName();
    }

    @Override
    public double assess(Chunk item, SmallCell sc) throws Throwable {
        return Utils.assessOnlyPop(this, item, sc);
    }

    @Override
    public int cacheDecision(
            SimulationBaseRunner sim, CachingUser cu, Collection<Chunk> requestChunks, SmallCell hostSC, SmallCell sc) throws Throwable {
        return EPC.cacheDecision(this, sim, cu, requestChunks, hostSC, hostSC);
//
//        int totalSizeCached = 0;
//        for (DocumentRequest nxtRequest : cu.getRequests()) {
//            for (Chunk nxtReqChunk : nxtRequest.referredContentDocument().chunks()) {
//
//                if (sc.isCachedBy(cu, this, nxtReqChunk)) {
//                    continue;
//                }
//
//                double cachePrice = sc.cachePricePoll(false, nxtReqChunk, this);
//                if (assess(nxtReqChunk, sc) >= cachePrice) {
//
//                    if (sc.isCached(this, nxtReqChunk)) {
//                        sc.addCacher(cu, this, nxtReqChunk);
//                        continue;
//                    }// otherwise, it may need to evict:
//
//                    if (!Utils.isSpaceAvail(this, sc, nxtReqChunk.sizeInBytes())) { //since no replacement policy supported
//                        continue;//cannot add this item
//                    }
//
//                    totalSizeCached += nxtReqChunk.sizeInBytes();
//                    sc.cacheItemAttempt(cu, this, nxtReqChunk);
//                }
//
//            }
//        }
//        return totalSizeCached;
    }

}
