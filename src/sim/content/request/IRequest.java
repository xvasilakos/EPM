package sim.content.request;

import caching.base.AbstractCachingPolicy;
import java.util.List;
import java.util.Map;
import sim.content.Chunk;
import sim.space.users.User;

/**
 *
 * @author xvas
 */
public interface IRequest {

    /**
     * Predicts the chunks that will be requested.
     *
     * @param policy
     * @param handoverProb
     * @param expectedHandoffDuration
     * @param conf95HandoffDur
     * @param expectedResidenceDuration
     * @param conf95ResidenceDur
     * @param mcRateSliceBytes
     * @param bhRateSliceBytes
     * @param scRateSliceBytes
     * @return
     */
    public List<Chunk> predictChunks2Request(
            AbstractCachingPolicy policy, double handoverProb, boolean isSoft, double expectedHandoffDuration, double conf95HandoffDur, double expectedResidenceDuration, double conf95ResidenceDur, int mcRateSliceBytes, int bhRateSliceBytes, int scRateSliceBytes);

    /**
     * Consumes for each caching policy the remainder chunks.
     *
     * @param policy
     * @param mcRateSlice
     * @param fillInWithDownloadedFromMC
     *
     */
    void consumeChunksRemainderFromMC(AbstractCachingPolicy policy, double mcRateSlice,
            Map<AbstractCachingPolicy, List<Chunk>> fillInWithDownloadedFromMC);

    
     public void consumeTryAllAtOnceFromSC(
            Map<AbstractCachingPolicy, List<Chunk>> fillInWithCacheHits,
            Map<AbstractCachingPolicy, List<Chunk>> fillInWithDownloadedFromBH,
            Map<AbstractCachingPolicy, List<Chunk>> fillInWithMissedPerPolicy);
    
    
    void consumeTry(double mcRateSlice, Map<AbstractCachingPolicy, List<Chunk>> fillInWithDownloadedFromMC,
            double scRateSlice, Map<AbstractCachingPolicy, List<Chunk>> fillInWithCacheHits,
            double minSCorBHRateSlice, Map<AbstractCachingPolicy, List<Chunk>> fillInWithDownloadedFromBH,
            Map<AbstractCachingPolicy, List<Chunk>> fillInWithMissedPerPolicy);

    @Override
    boolean equals(Object obj);

    List<Chunk> getChunksCacheHitsHistory(AbstractCachingPolicy policy);

    Map<AbstractCachingPolicy, List<Chunk>> getChunksConsumedHistoryFromBH();

    List<Chunk> getChunksConsumedHistoryFromBH(AbstractCachingPolicy policy);

    Map<AbstractCachingPolicy, List<Chunk>> getChunksConsumedHistoryFromMCWhileConnectedToSC();

    List<Chunk> getChunksConsumedHistoryFromMCwSCConn(AbstractCachingPolicy policy);

    List<Chunk> getChunksConsumedHistoryFromMCBeforeEnteringSC(AbstractCachingPolicy policy);

    List<Chunk> getChunksConsumedHistoryFromMCAfterExitingSC(AbstractCachingPolicy policy);

    int getIssuedAtSimTime();

    User getRequesterUser();

    @Override
    int hashCode();

    long requesterUserID();

}
