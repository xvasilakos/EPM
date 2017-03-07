package caching.base;

import sim.content.Chunk;
import sim.space.cell.smallcell.SmallCell;

/**
 * Proactive Caching using POPularity with Cache decisions cancelation (CNC)
 * supported and no support for any cache replacement policies.
 *
 *
 * @author xvas
 */
public abstract class AbstractPop extends AbstractPricing implements IPop {

    public AbstractPop() {
    }

    public abstract double assess(Chunk item, SmallCell sc) throws Throwable;
    // throw new UnsupportedOperationException(); //@todo bad desing due to 
    // numerous additions and modifications throughout time prosoxh na 
    // eisaxuei endiamesh klash gia ths klasseis me onoma *.PopOnly
}
