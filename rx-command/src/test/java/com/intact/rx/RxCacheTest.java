package com.intact.rx;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.api.cache.CachePolicy;
import com.intact.rx.api.cache.RxSelection;
import com.intact.rx.api.rxrepo.RxRepository;
import com.intact.rx.api.rxrepo.RxRepositoryReader;
import com.intact.rx.api.rxrepo.RxRepositoryWriter;
import com.intact.rx.api.rxrepo.RxValueReader;
import com.intact.rx.core.cache.data.context.CacheMasterPolicy;
import com.intact.rx.core.cache.data.context.DataCachePolicy;
import com.intact.rx.core.cache.data.id.MasterCacheId;
import com.intact.rx.core.rxcache.Streamer;
import com.intact.rx.core.rxcache.StreamerBuilder;
import com.intact.rx.core.rxcache.StreamerGroup;
import com.intact.rx.core.rxcircuit.breaker.CircuitBreakerPolicy;
import com.intact.rx.core.rxcircuit.breaker.CircuitId;
import com.intact.rx.core.rxrepo.CachedRepository;
import com.intact.rx.core.rxrepo.RepositoryConfig;
import com.intact.rx.testdata.command.Result;
import com.intact.rx.testdata.command.ResultSelectionObserver;
import com.intact.rx.policy.*;

class RxCacheTest {

    private static final int defaultTimeoutMsecs = 30000;

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(RxCacheTest.class);

    @Test
    void testStreamer() {
        StreamerBuilder builder = Streamer.forUUIDCache().build();

        builder.sequential().get(Result.class, Collections::emptyMap);

        StreamerGroup parallel = builder.parallel();

        for (int i = 0; i < 10; i++) {
            final int a = i;
            parallel.get(Result.class, () -> Map.of(a, new Result(a, "Success")));
        }

        Streamer streamer = builder.streamer();

        RxSelection<Integer, Result> selection = streamer
                .<Integer, Result>cache(Result.class)
                .computeSelection((dataObject, alreadyMember) -> dataObject.getId() == 5);

        selection.connect(new ResultSelectionObserver());

        List<Result> results = streamer
                .subscribe()
                .waitFor(Long.MAX_VALUE)
                .cache(Result.class)
                .readAsList();

        assertEquals(10, results.size());
        assertEquals(1, selection.size());
    }

    @Test
    void testTransformation() {
        StreamerBuilder builder = Streamer.forUUIDCache().build();

        builder.sequential().get(Result.class, Collections::emptyMap);

        StreamerGroup parallel = builder.parallel();

        for (int i = 0; i < 10; i++) {
            final int a = i;
            parallel.get(Result.class, () -> Map.of(a, new Result(a, "Success")));
        }

        Streamer streamer = builder.streamer();

        RxSelection<Integer, Integer> selection = streamer
                .<Integer, Result>cache(Result.class)
                .computeTransformation((Result value, boolean alreadyMember) -> value.getId() == 5 ? Optional.of(value.getId()) : Optional.empty());

        selection.onObjectInDo(integer -> log.debug("Got value {}", integer));

        List<Result> results = streamer
                .subscribe()
                .waitFor(Long.MAX_VALUE)
                .cache(Result.class)
                .readAsList();

        assertEquals(10, results.size());
        assertEquals(1, selection.size());
    }

    @Test
    void testScopedRepository() {
        String repositoryKey = "akey";
        CachedRepository<Integer, Result> repository = CachedRepository.withIsolatedRequests(Result.class);

        RxRepositoryReader<Integer, Result> reader = repository.requestFor(
                repositoryKey,
                () -> {
                    StreamerBuilder builder = Streamer.forCache(repository.getMasterCacheId()).build();

                    {
                        StreamerGroup parallel = builder.parallel();
                        for (int i = 0; i < 10; i++) {
                            final int a = i;
                            parallel.get(Result.class, () -> Map.of(a, new Result(a, "Success")));
                        }
                    }

                    {
                        StreamerGroup parallel = builder.parallel();

                        for (int i = 10; i < 20; i++) {
                            final int a = i;
                            parallel.get(Result.class, () -> Map.of(a, new Result(a, "Success")));
                        }
                    }

                    {
                        StreamerGroup parallel = builder.parallel();

                        for (int i = 20; i < 30; i++) {
                            final int a = i;
                            parallel.get(Result.class, () -> Map.of(a, new Result(a, "Success")));
                        }
                    }

                    {
                        StreamerGroup parallel = builder.parallel();

                        for (int i = 30; i < 40; i++) {
                            final int a = i;
                            parallel.get(Result.class, () -> Map.of(a, new Result(a, "Success")));
                        }
                    }

                    {
                        StreamerGroup parallel = builder.parallel();

                        for (int i = 40; i < 50; i++) {
                            final int a = i;
                            parallel.get(Result.class, () -> Map.of(a, new Result(a, "Success")));
                        }
                    }

                    {
                        StreamerGroup parallel = builder.parallel();

                        for (int i = 50; i < 60; i++) {
                            final int a = i;
                            parallel.get(Result.class, () -> Map.of(a, new Result(a, "Success")));
                        }
                    }

                    return builder.streamer();
                });


        for (int i = 0; i < 60; i++) {
            assertTrue(reader.computeIfAbsent(i, defaultTimeoutMsecs).isPresent());
        }

        // Streamer is gone after finished, therefore reader is null
        Optional<RxRepositoryReader<Integer, Result>> repositoryReader = repository.find(repositoryKey);
        assertFalse(repositoryReader.isPresent());
    }

    @Test
    void testRepositoryWriter() {
        CachePolicy cachePolicy = CachePolicy.create(CacheMasterPolicy.validForever(), DataCachePolicy.unlimitedForever());
        String repositoryKey = "okey";

        CachedRepository<Integer, Result> repository = CachedRepository.withIsolatedRequests(Result.class, cachePolicy);

        RxRepositoryWriter<Integer, Result> writer = repository.writeRequestFor(
                MasterCacheId.create(repositoryKey),
                (a, b) -> true
        );

        boolean isPut = writer.put(2, new Result(2, "yeah"), defaultTimeoutMsecs);

        assertTrue(isPut);
    }

    @Test
    void testMultipleSubscribesAndGets() {
        final long[] cnt = {0};
        final long[] start = {System.currentTimeMillis()};
        final int key = ThreadLocalRandom.current().nextInt();

        RxRepository<Integer, Result> repository = CachedRepository.withIsolatedRequests(Result.class);
        RxRepositoryReader<Integer, Result> reader =
                repository
                        .reusableRequestFor(
                                UUID.randomUUID(),
                                () -> Streamer.forCache(repository.getMasterCacheId()).build()
                                        .sequential()
                                        .get(Result.class, () -> {
                                            ++cnt[0];
                                            if (cnt[0] % 10000 == 0) {
                                                log.debug("Counting {}, time {} msecs", cnt[0], System.currentTimeMillis() - start[0]);
                                                start[0] = System.currentTimeMillis();
                                            }
                                            return Map.of(key, new Result(key, "1"));
                                        })
                                        .done())
                        .onErrorDo(throwable -> log.error("Something went wrong!", throwable));

        // Get and remove value causes multiple subscribes
        long rounds = 100;
        for (int i = 0; i < rounds; i++) {
            try {
                Optional<Result> result = reader.computeIfAbsent(key, defaultTimeoutMsecs);
                assertTrue(result.isPresent(), "Counted " + cnt[0]);
            } catch (Exception e) {
                log.error("Something went wrong!", e);
                throw e;
            }
            Optional<Result> remove = reader.cache().take(key);
            assertTrue(remove.isPresent());
            assertFalse(reader.cache().read(key).isPresent());
        }

        assertEquals(cnt[0], rounds, "Comparison of executions vs iterations (executions be equal). " + cnt[0] + " == " + rounds + " ? " + (cnt[0] == rounds));
    }

    @Test
    @Disabled
    void paralleltestMultipleSubscribesAndGets() {
        Streamer.forUUIDCache().withCommandTimeout(Timeout.ofMinutes(5)).build()
                .parallel()
                .run(this::testMultipleSubscribesAndGets)
                .run(this::testMultipleSubscribesAndGets)
                .run(this::testMultipleSubscribesAndGets)
                .run(this::testMultipleSubscribesAndGets)
                .run(this::testMultipleSubscribesAndGets)
                .run(this::testMultipleSubscribesAndGets)
                .subscribe()
                .waitFor(Long.MAX_VALUE);
    }

    @Test
    void testSubscribes() {
        final int[] cnt = {0};
        int key = 123456;
        CachedRepository<Integer, Result> repository = CachedRepository.withIsolatedRequests(Result.class);

        RxRepositoryReader<Integer, Result> reader = repository.requestFor(1L, () ->
                Streamer.forCache(repository.getMasterCacheId()).build()
                        .sequential()
                        .get(Result.class, () -> {
                            ++cnt[0];
                            return Map.of(key, new Result(key, "1"));
                        })
                        .done()
        );

        int rounds = 100;
        for (int i = 0; i < rounds; i++) {
            reader.computeIfAbsent(key, defaultTimeoutMsecs);
        }

        assertEquals(1, cnt[0], "Comparison of executions vs iterations (executions should be 1). " + cnt[0] + " == " + rounds + " ? " + (cnt[0] == rounds));
    }

    @Test
    @Disabled
    void stressTestMultipleSubscribes() {
        for (int k = 0; k < 100; k++) {
            testMultipleSubscribesAndGets();
        }
    }

    @Test
    @Disabled
    void parallelTestSubscribes() {
        Streamer.forUUIDCache().withCommandTimeout(Timeout.ofMinutes(5)).build()
                .parallel()
                .run(this::testSubscribes)
                .run(this::testSubscribes)
                .run(this::testSubscribes)
                .run(this::testSubscribes)
                .run(this::testSubscribes)
                .run(this::testSubscribes)
                .subscribe()
                .waitFor(Long.MAX_VALUE);
    }

    @Test
    void testShouldGetValue() {
        Long test = CachedRepository
                .<Long, Long>forType(Long.class).build()
                .computeIfAbsent(3L, k -> k)
                .orElse(3L);

        assertEquals(3, test.longValue());
    }

    @Test
    void testFailToGetValue() {
        Long nullValue = null;
        try {
            CachedRepository<Long, Long> repository = CachedRepository
                    .<Long, Long>forType(Long.class)
                    .withCommandAttempt(Attempt.once())
                    .withThreadPoolConfig("test-thread", 2)
                    .build();
            nullValue = repository.computeValueIfAbsent(3L, RxCacheTest::getFail);
        } catch (Throwable throwable) {
            log.info("Getting value failed as expected", throwable);
        }

        assertNull(nullValue);
    }

    @Test
    void testCompositions() {
        CachedRepository<Long, Long> repository = CachedRepository
                .<Long, Long>forType(Long.class)
                .withCommandAttempt(Attempt.once())
                .withThreadPoolConfig("test-thread", 2)
                .build();

        RxRepositoryReader<Long, Long> reader = repository.uuidRequestFor(
                () -> Streamer.forCache(repository.getMasterCacheId())
                        .build()
                        .parallel()
                        .<Long, Long>compose(Long.class)
                        .get(() -> {
                            throw new IllegalStateException("I fail on purpose.");
                        })
                        .withRetryInterval(Interval.ofMillis(300))
                        .withAttempt(Attempt.retry(3))
                        .fallbackGet(() -> Map.of(2L, 2L))
                        .build()
                        .done());

        Optional<Long> value = reader.computeIfAbsent(2L, defaultTimeoutMsecs);

        assertTrue(value.isPresent());
        //noinspection OptionalGetWithoutIsPresent
        assertEquals(2L, (long) value.get());
        assertEquals(0, reader.getStatus().getExecutionStatus().getCount().getNumFailures());
    }


    @Test
    void testActorCompositions() {
        Streamer streamer = Streamer.forUUIDCache().build()
                .parallel()
                .<Long, Long>compose(Long.class).withDetails()
                .skipGetIfCached(2L)
                .elseIfGet(resultMap -> resultMap.map(m -> m.containsKey(2L)).orElse(false), () -> Map.of(3L, 3L))
                .get(() -> Map.of(2L, 2L))
                .withAttempt(Attempt.retry(3))
                .fallbackGet(() -> Map.of(1L, 1L))
                .build()
                .parallel()
                .<Long, Long>compose(Long.class)
                .mapKey(Long.class, 3L, (key, value) -> Map.of(key + 2L, value.orElse(2L) + key))
                .build()
                .done();

        streamer.subscribe().waitFor(defaultTimeoutMsecs)
                .subscribe().waitFor(defaultTimeoutMsecs);

        assertEquals(2, streamer.cache(Long.class).size());
    }

    @Test
    void testNoCaching() {
        boolean isSuccess =
                Streamer.forUUIDCache().build()
                        .parallel()
                        .run(() -> log.debug("Doing something but no caching"))
                        .subscribe()
                        .waitFor(defaultTimeoutMsecs)
                        .isSuccess();

        assertTrue(isSuccess);
    }

    @Test
    void testCircuit() {
        final int[] halfOpenCnt = {0};

        RxRepository<Long, Long> repository =
                CachedRepository.<Long, Long>forType(Long.class)
                        .withRepositoryConfig(RepositoryConfig.builder().withCircuitBreakerPolicy(CircuitBreakerPolicy.create(MaxLimit.withLimit(3), Timeout.ofSeconds(3), Duration.ofMinutes(30))).withCircuitBreakerIsolatedById("testCircuit").build())
                        .withFaultPolicy(FaultPolicy.neverThrowWhenOptional())
                        .build();

        repository.circuit().onHalfOpenDo(circuitId -> log.info("half open circuit {}", ++halfOpenCnt[0]));

        for (int k = 0; k < 3; k++) {
            for (int i = 0; i < 10; i++) {
                repository
                        .uuidRequestValueFor(ThreadLocalRandom.current().nextLong(), aLong -> {
                            sleep(1000);
                            throw new IllegalStateException("I fail on purpose.");
                        })
                        .onSubscribeDo(subscription -> log.info("Subscribed {}", subscription))
                        .onNextDo(aLong -> log.info("Got {}", aLong))
                        .onErrorDo(throwable -> log.error("Error! ", throwable))
                        .onCompleteDo(() -> log.info("Completed"))
                        .computeIfAbsent(Long.MAX_VALUE);
            }
        }

        sleep(3000);

        for (int i = 0; i < 10; i++) {
            long key = ThreadLocalRandom.current().nextLong();
            repository
                    .uuidRequestValueFor(key, aLong -> {
                        log.info("Yes I am executing");
                        return key;
                    })
                    .onSubscribeDo(subscription -> log.info("Subscribed {}", subscription))
                    .onNextDo(aLong -> log.info("Got {}", aLong))
                    .onErrorDo(throwable -> log.error("Error! ", throwable))
                    .onCompleteDo(() -> log.info("Completed"))
                    .computeIfAbsent(1000);
        }

        assertEquals(1, halfOpenCnt[0]);
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignore) {
        }
    }

    @Test
    @Disabled
    void testParallelCircuit() {
        Streamer streamer = Streamer.forUUIDCache().withCommandTimeout(Timeout.ofMinutes(5)).build()
                .parallel()
                .run(this::testCircuit)
                .run(this::testCircuit)
                .run(this::testCircuit)
                .run(this::testCircuit)
                .subscribe()
                .waitFor(Long.MAX_VALUE);

        assertTrue(streamer.getStatus().isSuccess());
        assertEquals(1, streamer.getStatus().finishedGroups().size(), "Expected 1 group to finish. Was " + streamer.getStatus().finishedGroups().size());
    }

    @Test
    void testAsync() {
        final long[] cnt = {0};
        final long[] asyncCnt = {100};

        Streamer streamer = Streamer.forUUIDCache().build()
                .async()
                .get(Long.class, () -> {
                    sleep(1000);
                    return Map.of(++asyncCnt[0], 2L);
                })
                .sequential()
                .get(Long.class, () -> Map.of(++cnt[0], 1L))
                .get(Long.class, () -> Map.of(++cnt[0], 3L))
                .runOn(Long.class, cache -> {
                    log.info("First: {}", cache.size());
                    assertEquals(2, cache.size());
                })
                .syncpoint()
                .sequential()
                .runOn(Long.class, cache -> {
                    log.info("Second: {}", cache.size());
                    assertEquals(3, cache.size());
                })
                .sequential()
                .get(Long.class, () -> Map.of(++cnt[0], 1L))
                .get(Long.class, () -> Map.of(++cnt[0], 3L))
                .sequential()
                .runOn(Long.class, cache -> {
                    log.info("Third : {}", cache.size());
                    assertEquals(5, cache.size());
                })
                .async()
                .get(Long.class, () -> {
                    sleep(1000);
                    return Map.of(++asyncCnt[0], 2L);
                })
                .syncpoint()
                .async()
                .get(Long.class, () -> {
                    sleep(1000);
                    return Map.of(++asyncCnt[0], 2L);
                })
                .syncpoint()
                .parallel()
                .runOn(Long.class, cache -> {
                    log.info("Fourth : {}", cache.size());
                    assertEquals(7, cache.size());
                })
                .subscribe()
                .waitFor(defaultTimeoutMsecs);

        assertTrue(streamer.getStatus().isSuccess());
        assertEquals(7, streamer.cache(Long.class).size());
    }

    @Test
    void testSequential() {
        Optional<Long> aLong = Streamer.forUUIDCache().build()
                .sequential()
                .getValue(Long.class, () -> 1L)
                .getValue(Long.class, () -> 1L)
                .getValue(Long.class, () -> 1L)
                .getValue(Long.class, () -> 1L)
                .getValue(Long.class, () -> 1L)
                .getValue(Long.class, () -> 1L)
                .getValue(Long.class, () -> 1L)
                .getValue(Long.class, () -> 1L)
                .getValue(Long.class, () -> 1L)
                .getValue(Long.class, () -> 2L)
                .done()
                .computeIfAbsent(Long.class, 2L, defaultTimeoutMsecs);

        assertTrue(aLong.isPresent());
    }


    @Test
    void testCircuitCommand() {
        final int[] failCount = {0};

        RxRepository<Long, Long> repository = CachedRepository
                .<Long, Long>forType(Long.class)
                .withCommandAttempt(Attempt.retry(6))
                .withCommandCircuitBreakerPolicy(CircuitBreakerPolicy.create(MaxLimit.withLimit(2), Timeout.ofSeconds(2), Duration.ofMinutes(30)))
                .withCommandCircuitBreakerId(CircuitId.sharedById("testCircuitCommand"))
                .build();

        Optional<Long> value = repository.computeIfAbsent(1L, key -> ++failCount[0] > 3 ? 1L : getFail(key));
        assertTrue(value.isPresent());
    }

    @Test
    void testRepoCallback() {
        final int[] callbackCount = {0};

        RxRepository<Long, Long> repository = CachedRepository.<Long, Long>forType(Long.class).build();

        RxValueReader<Long> reader = repository
                .requestValueFor(1L, k -> 1L)
                .onSubscribeDo(subscription -> log.info("{}: Subscribed {}", ++callbackCount[0], subscription))
                .onNextDo(aLong -> log.info("{}: Got {}", ++callbackCount[0], aLong))
                .onCompleteDo(() -> log.info("{}: Completed", ++callbackCount[0]));

        reader.accessCached().onObjectCreatedDo(aLong -> log.info("{}: Object created", ++callbackCount[0]));
        reader.subscribe().waitFor(defaultTimeoutMsecs);
        assertEquals(4, callbackCount[0]);
    }

    private static Long getFail(Long key) {
        throw new IllegalStateException("I fail on purpose. Key=" + key);
    }
}
