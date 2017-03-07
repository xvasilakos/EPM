package sim.content;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import utils.ISynopsisString;
import sim.run.SimulationBaseRunner;

/**
 * Used for the records loaded by the documents file of the loaded trace.
 */
public class ContentDocument extends AbstractContent implements Comparable<ContentDocument>, ISynopsisString {

    private final int _totalNumberOfRequests;
    private final int _appType;
    private final SortedMap<Long, Chunk> _chunksInSequence;

    public ContentDocument(long id, SimulationBaseRunner sim, long sizeInBytes,
            int totalRequests, int appType) {
        super(id, sim, sizeInBytes);

        SortedMap tmpChunksInSequence = new TreeMap();
        long chunkSizeInBytes = sim.chunkSizeInBytes();

        int numOfCompleteChunks = (int) (sizeInBytes() / chunkSizeInBytes);
        int remainderChunkSize = (int) (sizeInBytes() % chunkSizeInBytes);
        int totalChunks = numOfCompleteChunks + (remainderChunkSize > 0 ? 1 : 0);

        //<editor-fold defaultstate="collapsed" desc="logging">
        java.util.logging.Logger.getLogger(getClass().getCanonicalName()).
                log(Level.FINER, "\n chunking content id {0} with size {1}MB into {2} chunks:", new Object[]{getID(), sizeInMBs(), totalChunks});
//</editor-fold>

        long seqNum = 0;
        while (seqNum < numOfCompleteChunks) {
            Chunk requestedChunk = new Chunk(this, chunkSizeInBytes, ++seqNum);
            tmpChunksInSequence.put(seqNum, requestedChunk);
            //<editor-fold defaultstate="collapsed" desc="logging">
            if (0.25 * numOfCompleteChunks % seqNum == 0) {
                java.util.logging.Logger.getLogger(getClass().getCanonicalName()).
                        log(Level.FINEST, "{0}% chunks created..", (1000.0 * seqNum / totalChunks / 10.0));
            }
//</editor-fold>
        }
        if (remainderChunkSize > 0) {// the left-over chunk..
            Chunk requestedChunk = new Chunk(this, remainderChunkSize, ++seqNum);
            tmpChunksInSequence.put(seqNum, requestedChunk);
            //<editor-fold defaultstate="collapsed" desc="logging">
            java.util.logging.Logger.getLogger(getClass().getCanonicalName()).
                    log(Level.FINEST, "100%.. Chunking process completed! Remainder chunk size: {0}MB", requestedChunk.sizeInMBs());
//</editor-fold>
        }
        _totalNumberOfRequests = totalRequests;
        _appType = appType;

        this._chunksInSequence = Collections.unmodifiableSortedMap(tmpChunksInSequence);
    }

    @Override
    public String toSynopsisString() {
        return "<id=" + getID()
                + "\t#requested=" + getTotalNumberOfRequests()
                + "\tsize=" + sizeInMBs() + "MB\tappType=" + getAppType() + ">";
    }

    @Override
    public ContentDocument referredContentDocument() {
        return getSim().getTrcDocs().get(getID());
    }

    @Override
    public String toString() {
        StringBuilder chunksInSequenceSynopsis = new StringBuilder();
        Iterator<Chunk> it = _chunksInSequence.values().iterator();
        while (it.hasNext()) {
            chunksInSequenceSynopsis.append(it.next().toSynopsisString());
        }

        return toSynopsisString()
                + "; \n\t chunks in ascending order of sequence number: "
                + chunksInSequenceSynopsis.toString();
    }

    /**
     * @return the number of requests of the item
     */
    public int getTotalNumberOfRequests() {
        return _totalNumberOfRequests;
    }

    /**
     * @return the application type of the item
     */
    public int getAppType() {
        return _appType;
    }

    @Override
    public int compareTo(ContentDocument t) {
        //descending order
        return (int) (t.getID() - this.getID());
    }

    public final SortedMap<Long, Chunk> getChunksInSequence() {
        return Collections.unmodifiableSortedMap(_chunksInSequence);
    }

    /**
     * @return chunks that are iterated in ascending order of sequence number
     */
    public Collection<Chunk> chunks() {
        return Collections.unmodifiableCollection(_chunksInSequence.values());
    }

    public Collection<Chunk> chunksFromSeqNum(long seqNum) {
        return Collections.unmodifiableCollection(_chunksInSequence.headMap(seqNum).values());
    }

    public Chunk getChunkWithSequenceNum(long seqNum) {
        return _chunksInSequence.get(seqNum);
    }

    public int totalNumberOfChunks() {
        return _chunksInSequence.size();
    }

    public void setCDNCached() {
        _costOfRmtTransfer *= .25;
        _costOfSCWireless *= .25;
        _costOfMCWireless *= .25;

    }

}
