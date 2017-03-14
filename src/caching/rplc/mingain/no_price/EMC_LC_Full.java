package caching.rplc.mingain.no_price;

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
public class EMC_LC_Full extends caching.base.no_price.AbstractGainRplc implements IEMC {

    private static final EMC_LC_Full SINGLETON = new EMC_LC_Full();

    public static EMC_LC_Full instance() {
        return SINGLETON;
    }

    public EMC_LC_Full() {
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
