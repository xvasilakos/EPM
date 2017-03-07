package caching.interfaces.incremental;

import sim.content.Chunk;
import sim.space.cell.smallcell.SmallCell;
import sim.space.users.CachingUser;

/**
 * Base interface for EPC methods implementations.
 *
 * @author xvas
 */
public interface IIncremental extends IIncrementalBase {

    public abstract double assess(CachingUser mu, Chunk item, SmallCell sc) throws Throwable;
}
