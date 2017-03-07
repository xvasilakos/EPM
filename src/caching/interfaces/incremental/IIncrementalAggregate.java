package caching.interfaces.incremental;

import sim.content.Chunk;
import sim.space.cell.smallcell.SmallCell;

/**
 * Base interface for EPC methods implementations.
 *
 * @author xvas
 */
public interface IIncrementalAggregate extends IIncrementalBase {

    public abstract double assess(Chunk item, SmallCell sc) throws Throwable;

}
