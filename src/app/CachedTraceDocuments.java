package app;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import sim.ISimulationMember;
import sim.content.ContentDocument;

/**
 * Resources that are common between different different simulations.
 *
 * Careful, synchronized sharing of document resources for the sake of memory
 * usage.
 *
 * @author xvas
 */
public class CachedTraceDocuments {

    private final ReadWriteLock _lock;
    private final Lock _writeLock;

    private final Logger _logger;

    private final Map<File, Map<String, ContentDocument>> _documentsPerTrcFile;
    private final Map<File, List<Long>> _documentsMeta;

    public CachedTraceDocuments() {
        this._logger = Logger.getLogger(CachedTraceDocuments.class.getName());
        this._lock = new ReentrantReadWriteLock();
        this._writeLock = _lock.writeLock();

        this._documentsPerTrcFile = new HashMap<>(5);
        this._documentsMeta = new HashMap<>(5);
    }

    public void acquireWriteLock() {
        _writeLock.lock();
    }

    public void releaseWriteLock() {
        _writeLock.unlock();
    }

    /**
     * @param f
     * @param id
     * @param sm
     * @return the the ContentDocument to which the specified id is mapped, or
     * null if underlying map contains no mapping for the id
     */
    public synchronized ContentDocument getDocument(File f, String id, ISimulationMember sm) {
        ContentDocument doc;
        Map<String, ContentDocument> documents = _documentsPerTrcFile.get(f);
        if (documents == null) {
            return null;
        }

        doc = documents.get(id);
        return doc;
    }

    public synchronized void addDocument(File f, String theID, ContentDocument theDoc, ISimulationMember sm) {
        Map<String, ContentDocument> documents = _documentsPerTrcFile.get(f);
        if (documents != null) {
            if (documents.put(theID, theDoc) != null) {
                _logger.log(Level.WARNING,
                        "Document is already considered: {0}", theDoc.toString());
            } else {
//xxx                sm.getSim().getStatsHandle().updtSCCmpt6(-1, "MEMSAVED");
            }
        } else {
            documents = new HashMap<>(440);
            documents.put(theID, theDoc);
//            StatsHandling statsHandle = sm.getSim().getStatsHandle();
            _documentsPerTrcFile.put(f, documents);
        }
    }

    public synchronized boolean contains(File f, String id) {
        return _documentsPerTrcFile.containsKey(f) && _documentsPerTrcFile.get(f).containsKey(id);
    }

    public synchronized boolean contains(File f) {
        return _documentsPerTrcFile.containsKey(f);
    }

    public synchronized Map<String, ContentDocument> getDocumentsOfTrace(
            File f, ISimulationMember sm) {
        Map<String, ContentDocument> docs = _documentsPerTrcFile.get(f);
        if (docs != null) {
            _logger.log(Level.INFO,
                    "Loaded {0} cross-sim cached content documents from file {1}",
                    new Object[]{docs.size(), f.getAbsolutePath()});


            
        }
        return docs;
    }

    public synchronized void removeDocumentsOfTrace(File f) {
        _documentsPerTrcFile.remove(f);
        _documentsMeta.remove(f);
    }

    /**
     * @return the _documentsMeta
     */
    public List<Long> getDocumentsMeta(File f) {
        return _documentsMeta.get(f);
    }

    public void addDocumentsMeta(File docFile, List<Long> meta) {
        _documentsMeta.put(docFile, meta);
    }
}
