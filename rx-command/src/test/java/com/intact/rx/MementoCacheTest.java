package com.intact.rx;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.RxConfig;
import com.intact.rx.api.RxDefault;
import com.intact.rx.api.cache.*;
import com.intact.rx.policy.LoanPolicy;
import com.intact.rx.policy.MementoPolicy;

class MementoCacheTest {
    private static final Logger log = LoggerFactory.getLogger(MementoCacheTest.class);

    @Test
    void shouldCompareAndWrite() {
        RxCache<String, String> cache = RxCacheAccess.cacheUUID(CachePolicy.fiveMinutesUnlimited());

        final String key = "1";
        final String yes = "yes";
        final String no = "no";

        boolean isUpdated = cache.compareAndWrite(key, () -> null, () -> yes);
        assertTrue(isUpdated);

        isUpdated = cache.compareAndWrite(key, () -> yes, () -> no);
        assertTrue(isUpdated);

        isUpdated = cache.compareAndWrite(key, () -> yes, () -> no);
        assertFalse(isUpdated);

        isUpdated = cache.compareAndWrite(key, () -> no, () -> yes);
        assertTrue(isUpdated);

        assertTrue(cache.read(key).map(s -> s.equals(yes)).orElse(false));
    }

    @Test
    void shouldReplaceOnly() {
        RxCache<String, String> cache = RxCacheAccess.cacheUUID(CachePolicy.fiveMinutesUnlimited());

        final String key = "1";
        final String yes = "yes";
        final String no = "no";

        Optional<String> previous = cache.replace(key, () -> yes);
        assertFalse(previous.isPresent());

        assertFalse(cache.read(key).isPresent());

        previous = cache.replace(key, () -> yes);
        assertFalse(previous.isPresent());

        previous = cache.compute(key, (s, s2) -> yes);
        assertFalse(previous.isPresent());

        previous = cache.replace(key, () -> no);
        assertTrue(previous.isPresent());

        assertTrue(cache.read(key).map(s -> s.equals(no)).orElse(false));
    }

    @Test
    void shouldMerge() {
        RxCache<String, String> cache = RxCacheAccess.cacheUUID(CachePolicy.fiveMinutesUnlimited());

        final String key = "1";
        final String yes = "yes";

        cache.merge(key, yes, String::concat);
        cache.merge(key, yes, String::concat);
        cache.merge(key, yes, String::concat);

        assertTrue(cache.read(key).map(s -> s.equals(yes + yes + yes)).orElse(false));
    }

    @Test
    void testAsJavaMap() {
        RxCache<String, String> cache = RxCacheAccess.cacheUUID(CachePolicy.fiveMinutesUnlimited());

        Map<String, String> stringMap = RxCollections.asMap(cache);
        for (int i = 0; i < 50; ++i) {
            stringMap.put(UUID.randomUUID().toString(), "value");
        }

        assertEquals(cache.size(), stringMap.size(), "should be same size");

        for (int i = 0; i < 50; ++i) {
            cache.write(UUID.randomUUID().toString(), "value");
        }

        assertEquals(cache.size(), stringMap.size(), "should be same size");
    }

    @Test
    void testAsJavaSet() {
        Set<String> set = RxSetData.<String>in("test").toSet(String.class);

        set.add("aTest");

        RxCache<String, String> cache = RxCacheData.in("test").cache(String.class);

        assertTrue(cache.read("aTest").isPresent());
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void testUndoRedoOnCache() {
        RxCache<String, String> cache = RxCacheData.uuid().withDefaultRxConfig(RxConfig.builder().withCacheMementoPolicy(new MementoPolicy(5, 5)).build()).cache(String.class);

        cache.onUndoDo(stringStringEntry -> log.info("Undo on {}", stringStringEntry));
        cache.onRedoDo(stringStringEntry -> log.info("Redo on {}", stringStringEntry));

        cache.onObjectCreatedDo((s, s2) -> log.info("Created {}={}", s, s2));
        cache.onObjectModifiedDo((s, s2) -> log.info("Modified {}={}", s, s2));
        cache.onObjectRemovedDo((s, s2) -> log.info("Removed {}={}", s, s2));

        for (int i = 40; i < 50; ++i) {
            cache.write(Integer.toString(i), "value");
        }

        // Note: undoing 49
        assertEquals("49", cache.undo().get().getKey());
        assertFalse(cache.read("49").isPresent());

        assertEquals("48", cache.undo().get().getKey());
        assertFalse(cache.read("48").isPresent());

        assertEquals("47", cache.undo().get().getKey());
        assertFalse(cache.read("47").isPresent());

        assertEquals("46", cache.undo().get().getKey());
        assertFalse(cache.read("46").isPresent());

        assertEquals("45", cache.undo().get().getKey());
        assertFalse(cache.read("45").isPresent());

        // Note: redoing 45, removing 44, etc
        assertEquals("45", cache.redo().get().getKey());
        assertTrue(cache.read("45").isPresent());

        assertEquals("46", cache.redo().get().getKey());
        assertTrue(cache.read("46").isPresent());

        assertEquals("47", cache.redo().get().getKey());
        assertTrue(cache.read("47").isPresent());

        assertEquals("48", cache.redo().get().getKey());
        assertTrue(cache.read("48").isPresent());

        assertEquals("49", cache.redo().get().getKey());
        assertTrue(cache.read("49").isPresent());

    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void testUndoRedoOnKey() {
        String key = "1";
        RxCache<String, String> cache = RxCacheData.uuid().withDefaultRxConfig(RxConfig.builder().withCachedObjectMementoPolicy(new MementoPolicy(5, 5)).build()).cache(String.class);

        cache.onUndoDo(stringStringEntry -> log.info("Undo on {}", stringStringEntry));
        cache.onRedoDo(stringStringEntry -> log.info("Redo on {}", stringStringEntry));

        cache.onObjectCreatedDo((s, s2) -> log.info("Created {}={}", s, s2));
        cache.onObjectModifiedDo((s, s2) -> log.info("Modified {}={}", s, s2));
        cache.onObjectRemovedDo((s, s2) -> log.info("Removed {}={}", s, s2));

        for (int i = 40; i < 50; ++i) {
            cache.write(key, Integer.toString(i));
        }

        // Note: undo 49, new value 48, etc
        assertEquals("49", cache.undo(key).get());
        assertEquals("48", cache.read(key).get());

        assertEquals("48", cache.undo(key).get());
        assertEquals("47", cache.read(key).get());

        assertEquals("47", cache.undo(key).get());
        assertEquals("46", cache.read(key).get());

        assertEquals("46", cache.undo(key).get());
        assertEquals("45", cache.read(key).get());

        assertEquals("45", cache.undo(key).get());
        assertEquals("44", cache.read(key).get());

        // Note: redoing 45, removing 44, etc
        assertEquals("44", cache.redo(key).get());
        assertEquals("45", cache.read(key).get());

        assertEquals("45", cache.redo(key).get());
        assertEquals("46", cache.read(key).get());

        assertEquals("46", cache.redo(key).get());
        assertEquals("47", cache.read(key).get());

        assertEquals("47", cache.redo(key).get());
        assertEquals("48", cache.read(key).get());

        assertEquals("48", cache.redo(key).get());
        assertEquals("49", cache.read(key).get());
    }

    @Test
    void testCachedValue() {
        RxCache<String, String> cache = RxCacheAccess.cacheUUID(RxDefault.getDefaultCachePolicy());

        for (int i = 0; i < 50; ++i) {
            cache.write(Integer.toString(i), "value");
        }

        try (RxValueAccess<String> valueAccess = cache.access("2")) {
            assertEquals("value", valueAccess.read().orElseThrow(() -> new IllegalStateException("Could not find value")));
        }
    }

    @Test
    void testLoanPolicy() {
        String key = "1";

        RxCache<String, String> cache = RxCacheAccess.cacheUUID(RxDefault.getDefaultCachePolicy());

        cache.onObjectCreatedDo((s, s2) -> log.info("Created {}={}", s, s2));
        cache.onObjectModifiedDo((s, s2) -> log.info("Modified {}={}", s, s2));
        cache.onObjectRemovedDo((s, s2) -> log.info("Removed {}={}", s, s2));

        Loaned<String> loan1 = cache.computeIfAbsentAndLoan(key, k -> "hello", LoanPolicy.exclusiveReadOnlyKeepOnNoLoan());
        Loaned<String> loan2 = cache.computeIfAbsentAndLoan(key, k -> "hello", LoanPolicy.exclusiveReadOnlyKeepOnNoLoan());

        assertTrue(loan1.read().isPresent());
        assertFalse(loan2.read().isPresent());

        loan1.returnLoan();

        assertTrue(loan2.read().isPresent());
    }

    @Test
    void testLoanPolicyExpireOnNoLoan() {
        String key = "1";

        RxCache<String, String> cache = RxCacheAccess.cacheUUID(RxDefault.getDefaultCachePolicy());

        cache.onObjectCreatedDo((s, s2) -> log.info("Created {}={}", s, s2));
        cache.onObjectModifiedDo((s, s2) -> log.info("Modified {}={}", s, s2));
        cache.onObjectRemovedDo((s, s2) -> log.info("Removed {}={}", s, s2));

        cache.write(key, "hello");

        Loaned<String> loan1 = cache.loan(key, LoanPolicy.exclusiveReadOnlyExpireOnNoLoan());
        Loaned<String> loan2 = cache.loan(key, LoanPolicy.readWriteExpireOnNoLoan());

        assertTrue(loan1.read().isPresent());
        assertFalse(loan2.read().isPresent());

        loan1.returnLoan();

        assertFalse(loan2.read().isPresent());
    }

    @Test
    void testValueEditor() {
        String key = "1";

        RxCache<String, String> cache = RxCacheAccess.cacheUUID(RxDefault.getDefaultCachePolicy());

        cache.onObjectCreatedDo((s, s2) -> log.info("Created {}={}", s, s2));
        cache.onObjectModifiedDo((s, s2) -> log.info("Modified {}={}", s, s2));
        cache.onObjectRemovedDo((s, s2) -> log.info("Removed {}={}", s, s2));

        cache.write(key, "hello");

        ValueEditor<String> editor = cache.edit(key);
        editor.write("hi");

        assertTrue(cache.read(key).map(s -> Objects.equals(s, "hello")).orElse(false));

        editor.commit();

        assertFalse(cache.read(key).map(s -> Objects.equals(s, "hello")).orElse(false));
        assertTrue(cache.read(key).map(s -> Objects.equals(s, "hi")).orElse(false));
    }

    @Test
    void testCacheEditor() {
        String key = "1";

        RxCache<String, String> cache = RxCacheAccess.cacheUUID(RxDefault.getDefaultCachePolicy());

        cache.onObjectCreatedDo((s, s2) -> log.info("Created {}={}", s, s2));
        cache.onObjectModifiedDo((s, s2) -> log.info("Modified {}={}", s, s2));
        cache.onObjectRemovedDo((s, s2) -> log.info("Removed {}={}", s, s2));

        cache.write(key, "hello");

        Editor<String, String> editor = cache.edit();
        editor.edit().write(key, "hi");

        assertTrue(cache.read(key).map(s -> Objects.equals(s, "hello")).orElse(false));

        editor.commit();

        assertFalse(cache.read(key).map(s -> Objects.equals(s, "hello")).orElse(false));
        assertTrue(cache.read(key).map(s -> Objects.equals(s, "hi")).orElse(false));
    }
}
