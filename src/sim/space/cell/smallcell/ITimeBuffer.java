/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sim.space.cell.smallcell;

import caching.base.AbstractCachingPolicy;
import java.util.Set;
import java.util.SortedMap;
import sim.content.Chunk;
import sim.space.users.CachingUser;
import utilities.Couple;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public interface ITimeBuffer {

    Set<Chunk> LRUAccessed();

    /**
     * Returns the next least recently used set of requests.
     *
     * If called with parameter next set to zero, then this method behaves
     * exactly like LRUAccessed(), returning the first record of the least
     * recently used requests. If parameter next is set to one, then it returns
     * the next set of least recently used request, and so forth.
     *
     * @param next
     * @return
     */
    public Set<Chunk> LRUAccessed(int next);

    public Set<Chunk> LRUCached();

    public int cachedTime(Chunk request);

    public Set<CachingUser> deallocate(Chunk request, CachingUser mu, AbstractCachingPolicy cachingMthd, SmallCell sc) throws UnsupportedOperationException, Throwable;

    /**
     * @return the time coupled with the requests that were least recently
     * accessed.
     */
    public Couple<Integer, Set<Chunk>> getLeastRecentlyAccessed();

    /**
     * @return the time coupled with the requests that were least recently
     * cached.
     */
    public Couple<Integer, Set<Chunk>> getLeastRecentlyCached();

    /**
     * @return the time coupled with the requests that were most recently
     * accessed.
     */
    public Couple<Integer, Set<Chunk>> getMostRecentlyAccessed();

    /**
     * @return the time coupled with the requests that were most recently
     * cached.
     */
    public Couple<Integer, Set<Chunk>> getMostRecentlyCached();

    public int mostRecentAccess(Chunk request);

    public boolean updtAccessTime(Chunk request);

    public long availableSpaceInBytes();

    public SortedMap<Integer, Set<Chunk>> getLRUMapping();

}
