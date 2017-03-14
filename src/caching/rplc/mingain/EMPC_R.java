package caching.rplc.mingain;

import caching.Utils;
import caching.base.IEMPC;
import sim.content.Chunk;
import sim.space.cell.smallcell.SmallCell;

/**
 * Efficient proactive caching with legacy popularity, using a least gain cache
 * replacement policy defined in class AbstractGainRplc. Unlike MinGainAvgProb,
 * this class implementation uses the sum of transition probabilities for
 * assessing the weighted delay gain of a cache decision.
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class EMPC_R extends caching.base.no_price.AbstractGainRplc implements IEMPC {

    private static final EMPC_R SINGLETON = new EMPC_R();

    public static EMPC_R instance() {
        return SINGLETON;
    }

    public EMPC_R() {
    }

    @Override
    public String nickName() {
        return "EMPC-R";
    }

    @Override
    public double assess(Chunk item, SmallCell sc) throws Throwable {
        return Utils.assessEMPC(item, sc, this);
    }

}
