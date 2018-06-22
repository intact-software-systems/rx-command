package com.intact.rx.core.cache.strategy;

import java.time.Duration;
import java.util.*;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.RxDefault;
import com.intact.rx.api.cache.RxCacheAccess;
import com.intact.rx.core.cache.data.CacheMaster;
import com.intact.rx.core.cache.data.DataCache;
import com.intact.rx.core.cache.data.ObjectRoot;
import com.intact.rx.core.cache.data.context.DataCachePolicy;

public final class CacheCleanupAlgorithms {
    private static final Logger log = LoggerFactory.getLogger(CacheCleanupAlgorithms.class);

    public static boolean cleanupCacheMaster(CacheMaster cacheMaster) {
        boolean removed = RxCacheAccess
                .find(cacheMaster.getDomainCacheId())
                .map(cacheFactory ->
                        cacheFactory.expireIf(
                                cacheMaster,
                                () -> cacheMaster.isExceededLifetime() || cacheMaster.isEmpty() && CachePolicyChecker.isInactive(cacheMaster.getAccessStatus(), RxDefault.getCacheInactiveTimeout())))
                .orElse(false);

        if (removed) {
            log.debug("Removed CacheMaster {}, time since last modified {} minutes", cacheMaster.getMasterCacheId(), Duration.ofMillis(cacheMaster.getAccessStatus().getTime().getTimeSinceModified()).toMinutes());
            cacheMaster.clearAll();
        }
        return removed;
    }

    public static <K> boolean defaultCacheCleanup(final DataCachePolicy policy, final DataCache<K, ?> dataCache) {
        requireNonNull(policy);
        requireNonNull(dataCache);

        // -----------------------------------------------------------
        // If lifespan is expired then expire datacache and objects
        // -----------------------------------------------------------
        if (dataCache.isExpired()) {
            dataCache.setExpired();
        }

        // -----------------------------------------------------------
        // Remove deleted and expired items first
        // -----------------------------------------------------------
        Map<K, ?> removed = dataCache.takeExpired();
        //log.info("{}: Removed {} objects", dataCache.getCacheId(), removed.size());

        // -----------------------------------------------------------
        // Eviction: LRU and LFU are well-known cache replacement strategies
        // -----------------------------------------------------------
        long numSamplesToRemove = dataCache.size() - policy.getResourceLimits().getMaxSamples();

        if (numSamplesToRemove > 0) {
            policy.getEvictionStrategy().perform(dataCache, numSamplesToRemove);
            //log.info(dataCache.getCacheHandle() + ": Removed n = " + numSamplesToRemove + ", keys = " + perform + ", cache = " + dataCache);
        }

        return !removed.isEmpty() || numSamplesToRemove > 0;
    }

    // ----------------------------------
    // Strategy algorithm
    // ----------------------------------

    /**
     * Removes n least frequently accessed/used entries from data-cache based on the access counter.
     */
    public static <K> Set<K> removeNLeastFrequentlyUsed(DataCache<K, ?> dataCache, long numSamplesToRemove) {
        if (numSamplesToRemove <= 0) {
            return Collections.emptySet();
        }

        // ----------------------------------------------
        // Get the n LFU entries and remove the n oldest entries
        // ----------------------------------------------
        Set<K> setOfLFUs = getNLeastFrequentlyUsedKeysPrivate(dataCache, numSamplesToRemove);

        setOfLFUs.forEach(dataCache::take);

        return setOfLFUs;
    }

    /**
     * N Least Frequently Used (LFU) items are returned.
     */
    private static <K> Set<K> getNLeastFrequentlyUsedKeysPrivate(DataCache<K, ?> dataCache, long numSamplesToRemove) {
        // ----------------------------------------------
        // Order entries according to number of times accessed
        // ----------------------------------------------
        SortedMap<Long, List<K>> LFUEntries = new TreeMap<>();

        // ----------------------------------------------
        // Iterate through all objects and insert to LFU map
        // ----------------------------------------------
        for (ObjectRoot<K, ?> obj : dataCache.getRoots()) {
            long readCount = obj.getStatus().getCount().getReadCount();

            List<K> listOfKeys = LFUEntries.get(readCount);

            // --------------------------------------------------------
            // No key == obj.getNumTimesAccessed() found so insert new
            // --------------------------------------------------------
            if (listOfKeys == null) {
                //noinspection ReuseOfLocalVariable
                listOfKeys = new ArrayList<>();
                boolean isInserted = listOfKeys.add(obj.getKey());

                // -- debug --
                assert isInserted;
                // -- debug --

                LFUEntries.put(readCount, listOfKeys);
            }
            // --------------------------------------------------------
            // Insert key to the list
            // --------------------------------------------------------
            else {
                boolean isInserted = listOfKeys.add(obj.getKey());

                // -- debug --
                assert isInserted;
                // -- debug --
            }
        }

        // ----------------------------------------------
        // Identify n least accessed (LeastFrequentlyUsed) entries
        // ----------------------------------------------
        Set<K> setKeys = new HashSet<>();

        for (Map.Entry<Long, List<K>> e : LFUEntries.entrySet()) {
            // ---------------------------------------------
            // Decremented when keys are added to setKeys
            // ---------------------------------------------
            if (numSamplesToRemove <= 0) {
                break;
            }

            for (K key : e.getValue()) {
                // ---------------------------------------------
                // Decremented when keys are added to setKeys
                // ---------------------------------------------
                if (numSamplesToRemove <= 0) {
                    break;
                }

                //noinspection AssignmentToMethodParameter
                numSamplesToRemove--;

                boolean isInserted = setKeys.add(key);

                // -- debug --
                assert isInserted;
                // -- debug --
            }
        }

        return setKeys;
    }


    // ----------------------------------
    // Strategy algorithm
    // ----------------------------------

    /**
     * Removes n oldest entries from data cache based on last access time.
     */
    public static <K> Set<K> removeNLeastRecentlyUsed(DataCache<K, ?> dataCache, long numSamplesToRemove) {
        if (numSamplesToRemove <= 0) {
            return Collections.emptySet();
        }

        // ----------------------------------------------
        // Get the n oldest entries
        // ----------------------------------------------
        Set<K> setOfLRUs = getNLeastRecentlyUsedKeys(dataCache, numSamplesToRemove);

        // ----------------------------------------------
        // Remove the n oldest entries
        // ----------------------------------------------
        setOfLRUs.forEach(dataCache::take);

        return setOfLRUs;
    }


    /**
     * N Least Recently Used (LRU) items are returned.
     */
    private static <K> Set<K> getNLeastRecentlyUsedKeys(DataCache<K, ?> dataCache, long numSamplesToRemove) {
        // ----------------------------------------------
        // Order entries according to age
        // ----------------------------------------------
        NavigableMap<Long, K> LRUEntries = new TreeMap<>();

        long currentTimeMs = System.currentTimeMillis();

        // ----------------------------------------------
        // Iterate through all objects and insert to LRU map
        // ----------------------------------------------
        for (ObjectRoot<K, ?> obj : dataCache.getRoots()) {
            long timeSinceAccessedMs = currentTimeMs - obj.getStatus().getTime().getReadTime();
            LRUEntries.put(timeSinceAccessedMs, obj.getKey());
        }

        // ----------------------------------------------
        // Identify n oldest (LeastRecentlyUsed) entries
        // ----------------------------------------------
        Set<K> setKeys = new HashSet<>();

        for (Map.Entry<Long, K> e : LRUEntries.descendingMap().entrySet()) {
            setKeys.add(e.getValue());

            //noinspection AssignmentToMethodParameter
            --numSamplesToRemove;
            if (numSamplesToRemove <= 0) {
                break;
            }
        }

        return setKeys;
    }

    private CacheCleanupAlgorithms() {
    }
}
