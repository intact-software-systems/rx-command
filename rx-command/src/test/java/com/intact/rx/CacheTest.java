package com.intact.rx;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.intact.rx.api.cache.*;
import com.intact.rx.core.cache.data.id.DataCacheId;
import com.intact.rx.core.cache.data.id.MasterCacheId;
import com.intact.rx.testdata.cache.SimpleCacheObserver;
import com.intact.rx.testdata.cache.StringObjectObserver;
import com.intact.rx.testdata.command.Result;

import static com.intact.rx.api.RxDefault.getDefaultCachePolicy;
import static com.intact.rx.api.RxDefault.getDefaultDomainCacheId;

@SuppressWarnings("ConstantConditions")
class CacheTest {

    @Test
    void testEmptyCache() {
        final Reader<Object, Result> cacheReader = RxCacheAccess.cacheUUID(getDefaultCachePolicy());
        assertTrue(cacheReader.readAsList().isEmpty());
    }

    @Test
    void testCacheWithValues() {
        final MasterCacheId masterCacheId = new MasterCacheId("testCacheWithValues");
        final Reader<Object, String> cacheReader = RxCacheAccess.cache(CacheHandle.create(getDefaultDomainCacheId(), masterCacheId, String.class), getDefaultCachePolicy());
        final Writer<Object, String> cacheWriter = RxCacheAccess.cache(CacheHandle.create(getDefaultDomainCacheId(), masterCacheId, String.class), getDefaultCachePolicy());
        cacheWriter.write(1, "first");
        cacheWriter.write(2, "second");
        assertEquals(2, cacheReader.size());
        assertEquals("first", cacheReader.read(1).get());
        assertEquals("second", cacheReader.read(2).get());
        cacheWriter.take(1);
        assertEquals(1, cacheReader.size());
        assertFalse(cacheReader.containsKey(1));
        cacheWriter.take(2);
        assertTrue(cacheReader.readAsList().isEmpty());
    }

    @Test
    void testCacheReaderWriter() {
        final RxCache<Object, String> cache = RxCacheAccess.cacheUUID(getDefaultCachePolicy());
        cache.write(1, "first");
        cache.write(2, "second");
        assertEquals(2, cache.size());
        assertEquals("first", cache.read(1).get());
        assertEquals("second", cache.read(2).get());
        cache.take(1);
        assertEquals(1, cache.size());
        assertFalse(cache.containsKey(1));
        cache.take(2);
        assertTrue(cache.readAsList().isEmpty());
    }

    @Test
    void testInvalidateCache() {
        final RxCache<Object, String> cache = RxCacheAccess.cacheUUID(getDefaultCachePolicy());
        cache.write(1, "first");
        cache.write(2, "second");
        cache.clear();
        assertTrue(cache.readAsList().isEmpty());
    }

    @Test
    void testUpdateCachedValue() {
        final RxCache<Object, String> cache = RxCacheAccess.cacheUUID(getDefaultCachePolicy());
        cache.write(1, "first");
        assertEquals("first", cache.read(1).get());
        cache.write(1, "updated");
        assertEquals("updated", cache.read(1).get());
    }

    @Test
    void testObjectObserver() {
        final StringObjectObserver observer = new StringObjectObserver();
        final RxCache<Object, String> cache = RxCacheAccess.cacheUUID(getDefaultCachePolicy());
        cache.addObjectObserver(observer);
        cache.write(1, "first");
        assertEquals("first", observer.getObject());
        cache.write(2, "second");
        assertEquals("second", observer.getObject());
        cache.write(1, "third");
        assertEquals("third", observer.getObject());
        cache.take(2);
        assertEquals("second", observer.getObject());
    }

    @Test
    void testCacheObserver() {
        final MasterCacheId cacheId = MasterCacheId.uuid();
        final SimpleCacheObserver observer = new SimpleCacheObserver();
        final DataCacheId dataCacheId = DataCacheId.create(String.class, cacheId);
        final RxCache<Integer, String> cache = RxCacheAccess.cache(CacheHandle.create(getDefaultDomainCacheId(), cacheId, String.class), getDefaultCachePolicy());
        cache.addCacheObserver(observer);
        cache.write(1, "first");
        assertEquals(observer.getId().getDataCacheId(), dataCacheId);
    }

    @Test
    void testMasterCacheExpiration() {
        final MasterCacheId masterCacheId = MasterCacheId.uuid();
        final RxCache<Object, String> cache = RxCacheAccess.cache(CacheHandle.create(getDefaultDomainCacheId(), masterCacheId, String.class), getDefaultCachePolicy());
        RxCacheAccess.expireDataCache(CacheHandle.create(getDefaultDomainCacheId(), masterCacheId, String.class));
        cache.write(1, "first");
        cache.write(2, "second");
        assertTrue(cache.isExpired());
    }

    @Test
    void testCacheExpiration() {
        final MasterCacheId masterCacheId = MasterCacheId.uuid();
        final RxCache<Integer, String> cache = RxCacheAccess.cache(CacheHandle.create(getDefaultDomainCacheId(), masterCacheId, String.class), getDefaultCachePolicy());
        final RxCache<Integer, Integer> cache2 = RxCacheAccess.cache(CacheHandle.create(getDefaultDomainCacheId(), masterCacheId, Integer.class), getDefaultCachePolicy());
        cache.write(1, "first");
        cache.write(2, "second");
        cache2.write(1, 1);
        RxCacheAccess.expireDataCache(CacheHandle.create(getDefaultDomainCacheId(), masterCacheId, String.class));
        assertTrue(cache.isExpired());
        assertTrue(!cache2.isExpired());
    }

}
