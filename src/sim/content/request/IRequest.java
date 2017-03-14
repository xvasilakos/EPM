package sim.content.request;

import caching.base.AbstractCachingModel;
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
            AbstractCachingModel policy, double handoverProb, boolean isSoft, double expectedHandoffDuration, double conf95HandoffDur, double expectedResidenceDuration, double conf95ResidenceDur, int mcRateSliceBytes, int bhRateSliceBytes, int scRateSliceBytes);

    /**
     * Consumes for each caching policy the remainder chunks.
     *
     * @param policy
     * @param mcRateSlice
     * @param fillInWithDownloadedFromMC
     *
     */
    void consumeChunksRemainderFromMC(AbstractCachingModel policy, double mcRateSlice,
            Map<AbstractCachingModel, List<Chunk>> fillInWithDownloadedFromMC);

    
     public void consumeTryAllAtOnceFromSC(
            Map<AbstractCachingModel, List<Chunk>> fillInWithCacheHits,
            Map<AbstractCachingModel, List<Chunk>> fillInWithDownloadedFromBH,
            Map<AbstractCachingModel, List<Chunk>> fillInWithMissedPerPolicy);
    
    
    void consumeTry(double mcRateSlice, Map<AbstractCachingModel, List<Chunk>> fillInWithDownloadedFromMC,
            double scRateSlice, Map<AbstractCachingModel, List<Chunk>> fillInWithCacheHits,
            double minSCorBHRateSlice, Map<AbstractCachingModel, List<Chunk>> fillInWithDownloadedFromBH,
            Map<AbstractCachingModel, List<Chunk>> fillInWithMissedPerPolicy);

    @Override
    boolean equals(Object obj);

    List<Chunk> getChunksCacheHitsHistory(AbstractCachingModel policy);

    Map<AbstractCachingModel, List<Chunk>> getChunksConsumedHistoryFromBH();

    List<Chunk> getChunksConsumedHistoryFromBH(AbstractCachingModel policy);

    Map<AbstractCachingModel, List<Chunk>> getChunksConsumedHistoryFromMCWhileConnectedToSC();

    List<Chunk> getChunksConsumedHistoryFromMCwSCConn(AbstractCachingModel policy);

    List<Chunk> getChunksConsumedHistoryFromMCBeforeEnteringSC(AbstractCachingModel policy);

    List<Chunk> getChunksConsumedHistoryFromMCAfterExitingSC(AbstractCachingModel policy);

    int getIssuedAtSimTime();

    User getRequesterUser();

    @Override
    int hashCode();

    long requesterUserID();

}
