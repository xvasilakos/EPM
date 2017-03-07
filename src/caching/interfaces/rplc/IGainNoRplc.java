package caching.interfaces.rplc;

import java.util.Collection;
import sim.run.SimulationBaseRunner;
import sim.content.Chunk;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;

/**
 * Base interface for EPC methods implementations.
 *
 * @author xvas
 */
public interface IGainNoRplc {

    /**
     * @return a string representation derived by the canonical name of this
     * object's class.
     */
    @Override
    public String toString();

    /**
     * Takes cache decisions for a small cell regarding the request by mobile
     * user mu that is currently hosted at small cell scHost.
     *
     * @param sim the value of sim
     * @param mu the mobile user.
     * @param requestChunks
     * @param hostSC
     * @param targetSC
     *
     * @return the int
     *
     */
    public int cacheDecision(SimulationBaseRunner sim, CachingUser mu, Collection<Chunk> requestChunks, SmallCell hostSC, SmallCell targetSC) throws Throwable;
}
