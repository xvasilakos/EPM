package caching;

import caching.base.AbstractCachingModel;
import java.util.Iterator;
import java.util.SortedSet;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.mobile.MobileUser;
import sim.content.ContentDocument;
import sim.content.Chunk;
import traces.dmdtrace.TraceLoader;

/**
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public final class MaxPop extends AbstractCachingModel {

    private static final MaxPop singleton = new MaxPop();

    /**
     * @return the singleton instance of this class according to its placement
     * in the hierarchy of AbstractMethod class descendants.
     */
    public static AbstractCachingModel instance() {
        return singleton;
    }

    private MaxPop() {
    }

    public void fillMaxPopCache(SmallCell sc, TraceLoader trc) throws Throwable {
        // max prioririty sorted
        SortedSet<ContentDocument> maxPopInfo = trc.getMaxPopDocuments();

        Iterator<ContentDocument> iterator = maxPopInfo.iterator();
        while (sc.buffAvailable(this) > 0 && iterator.hasNext()) {
            ContentDocument next = iterator.next();
            if (next.sizeInBytes() / sc.buffCapacity(this) > sc.getSimulation().getMaxPopCachingCutter()) {
                continue; // rule of the thumb: do not cache is it occupies too much space.
            }
            for (Chunk nxtChunk : next.chunks()) {
                sc.cacheItemAttempt(MobileUser.DUMMY_MU, this, nxtChunk);
            }
        }
    }

}
