package com.intact.rx;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.intact.rx.api.rxrepo.RxRepository;
import com.intact.rx.core.rxcircuit.rate.RateLimiterId;
import com.intact.rx.core.rxcircuit.rate.RateLimiterPolicy;
import com.intact.rx.core.rxrepo.CachedRepository;
import com.intact.rx.policy.Attempt;
import com.intact.rx.policy.FaultPolicy;
import com.intact.rx.policy.Frequency;
import com.intact.rx.policy.Interval;

class RateLimitTest {

    @Test
    void testCommandRateLimit() {
        final int[] failCount = {0};

        RxRepository<Long, Long> repository = CachedRepository
                .<Long, Long>forType(Long.class)
                .withCommandAttempt(Attempt.retry(10))
                .withCommandInterval(Interval.ofMillis(500))
                .withCommandRetryInterval(Interval.ofMillis(500))
                .withCommandRateLimiterPolicy(RateLimiterPolicy.timeBasedFilter(new Frequency(Duration.ofMillis(1000))))
                .withCommandRateLimiterId(RateLimiterId.isolated("testRateLimiter"))
                .build();

        Optional<Long> value = repository.computeIfAbsent(1L, key -> ++failCount[0] > 4 ? 1L : getFail(key));
        assertTrue(value.isPresent());
    }


    @Test
    void testRepositoryRateLimit() throws InterruptedException {
        RxRepository<Long, Long> repository = CachedRepository
                .<Long, Long>forType(Long.class)
                .withRepositoryRateLimiterPolicy(RateLimiterPolicy.timeBasedFilter(new Frequency(Duration.ofMillis(1000))))
                .withRepositoryRateLimiterSharedById("testRateLimiter")
                .withFaultPolicy(FaultPolicy.neverThrowWhenOptional())
                .build();

        for (int i = 0; i < 2; i++) {
            Optional<Long> val = repository.computeIfAbsent(ThreadLocalRandom.current().nextLong(), key -> key);
            assertTrue(val.isPresent());

            for (int j = 0; j < 10; j++) {
                Optional<Long> value = repository.computeIfAbsent(ThreadLocalRandom.current().nextLong(), key -> key);
                assertFalse(value.isPresent());
            }

            sleep(1000);
        }
    }

    private static Long getFail(Long key) {
        throw new IllegalStateException("I fail on purpose. Key=" + key);
    }
}
