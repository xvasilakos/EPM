package caching.rplc.mingain;

import caching.Utils;
import caching.base.IEMC;
import sim.space.cell.smallcell.SmallCell;
import sim.content.Chunk;

/**
 * Efficient proactive caching with legacy popularity, using a least gain cache
 * replacement policy defined in class AbstractGainRplc. Unlike MinGainAvgProb,
 * this class implementation uses the sum of transition probabilities for
 * assessing the weighted delay gain of a cache decision.
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class EMC_R extends caching.base.no_price.AbstractGainRplc implements IEMC {

    private static final EMC_R SINGLETON = new EMC_R();

    public static EMC_R instance() {
        return SINGLETON;
    }

    public EMC_R() {
    }

    @Override
    public String nickName() {
        return "EMC-R";
    }

    @Override
    public double assess(Chunk theChunk, SmallCell sc) throws Throwable {
        return Utils.assessEMC(theChunk, sc, this);
    }

}
