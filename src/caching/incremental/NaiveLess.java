package caching.incremental;

import java.util.Collection;
import sim.run.SimulationBaseRunner;
import sim.content.Chunk;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;

/**
 * Less naive than original Naive by ignoring request with minor probability
 * after parameter
 * #SimulationBaseRunner.CACHING_POLICIES__NAIVE__TYPE02__THREASHOLD
 *
 * @author xvas
 */
public final class NaiveLess extends Naive {

    private static final NaiveLess SINGLETON = new NaiveLess();

    public static NaiveLess instance() {
        return SINGLETON;
    }

    protected NaiveLess() {
    }

    @Override
    public String nickName() {
        return getClass().getName();
    }

    @Override
    public int cacheDecision(
            SimulationBaseRunner sim, CachingUser mu, Collection<Chunk> requestChunks, SmallCell hostSC, SmallCell targetSC) throws Throwable {

        if (!hostSC.hasAsNeighbor(targetSC)) {
            return 0;
        }

        return super.cacheDecision(sim, mu, requestChunks, hostSC, targetSC);
    }

}
