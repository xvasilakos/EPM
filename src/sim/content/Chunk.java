package sim.content;

import utils.ISynopsisString;

/**
 * @author xvas
 */
public class Chunk extends AbstractContent implements ISynopsisString {

    private final long _sequenceNum;
    private final ContentDocument _referedTraceDocument;

    protected Chunk(ContentDocument traceDocRecord, long size, long chunkSequenceNum) {
        super(hashCodeFor(chunkSequenceNum, traceDocRecord), traceDocRecord.getSim(), size);
        _sequenceNum = chunkSequenceNum;
        _referedTraceDocument = traceDocRecord;
    }

    @Override
    public final String toString() {
        StringBuilder bld = new StringBuilder();
        bld.
                append(super.toString()).
                append("\n\t").
                append(toSynopsisString()).
                append("; refers to Content:").
                append("\n\t\t").
                append(referredContentDocument().toString());
        return bld.toString();
    }

    /**
     *
     * @return
     */
    @Override
    public String toSynopsisString() {
        StringBuilder bld = new StringBuilder();
        bld.
                append("Chunk ID=").
                append(getID()).
                append("; sequence num=").
                append(_sequenceNum);
        return bld.toString();
    }

    /**
     * @return the _sequenceNum
     */
    public long getSequenceNum() {
        return _sequenceNum;
    }

    public static int hashCodeFor(long sequenceNumber, ContentDocument theContent) {
        int hash = 41;
        hash = 31 * hash + (int) (sequenceNumber ^ (sequenceNumber >>> 32));
        hash = 37 * hash + theContent.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        final Chunk other = (Chunk) obj;
        if (this._sequenceNum != other._sequenceNum) {
            return false;
        }
        if (!this._referedTraceDocument.equals(other._referedTraceDocument)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        // this is essentially the id as defined in the contrauctor
        return hashCodeFor(_sequenceNum, _referedTraceDocument);
    }

    /**
     *
     * @return
     */
    @Override
    public final ContentDocument referredContentDocument() {
        return _referedTraceDocument;
    }

}
