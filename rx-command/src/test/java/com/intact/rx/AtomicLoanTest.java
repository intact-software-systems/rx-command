package com.intact.rx;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.core.rxcache.Streamer;
import com.intact.rx.templates.AtomicLoan;
import com.intact.rx.templates.AtomicLoanMemento;
import com.intact.rx.templates.MementoReference;
import com.intact.rx.templates.Validate;

class AtomicLoanTest {
    private static final Logger log = LoggerFactory.getLogger(AtomicLoanTest.class);

    @Test
    void testAtomicLoan() {
        final long[] cnt = {0};

        AtomicLoan<String> atomicLoan = AtomicLoan.create(() -> {
            log.info("Caching {}", ++cnt[0]);
            sleep(100);
            return "Balle";
        }, Duration.ofMillis(100));

        Streamer.forUUIDCache()
                .build()
                .parallel()
                .run(atomicLoan::refresh)
                .run(atomicLoan::refresh)
                .run(atomicLoan::refresh)
                .run(atomicLoan::refresh)
                .run(atomicLoan::refresh)
                .run(atomicLoan::refresh)
                .run(atomicLoan::refresh)
                .run(atomicLoan::refresh)
                .run(atomicLoan::refresh)
                .run(atomicLoan::refresh)
                .run(atomicLoan::refresh)
                .run(atomicLoan::refresh)
                .run(atomicLoan::refresh)
                .run(atomicLoan::refresh)
                .run(atomicLoan::refresh)
                .run(atomicLoan::refresh)
                .run(atomicLoan::get)
                .run(atomicLoan::get)
                .run(atomicLoan::get)
                .run(atomicLoan::get)
                .subscribe()
                .waitFor(100000);

        atomicLoan.refresh(s -> s.map(s1 -> s1 + " Krakil").orElse("WRONG"));

        Validate.assertTrue(cnt[0] == 1);
        Validate.assertTrue(atomicLoan.read().map(s -> Objects.equals(s, "Balle Krakil")).orElse(false));
    }

    @Test
    void testAsyncAtomicLoan() throws Exception {
        final long[] cnt = {0};
        final String value = "Balle";

        AtomicLoan<String> atomicLoan = AtomicLoan.create(() -> {
            log.info("Caching {}", ++cnt[0]);
            return value;
        });

        CompletableFuture
                .supplyAsync(atomicLoan)
                .whenComplete((s, throwable) -> log.info("{}", s))
                .thenApply(s -> s + " Krakil")
                .whenComplete((s, throwable) -> log.info("{}", s))
                .get();

        Validate.assertTrue(atomicLoan.read().map(s -> Objects.equals(s, value)).orElse(false));
    }

    @Test
    void shouldLoanAndCreateMementos() {
        final long[] cnt = {0};

        AtomicLoanMemento<String> atomicLoan = AtomicLoanMemento.create(() -> {
            log.info("Caching {}", ++cnt[0]);
            return "Balle";
        }, Duration.ofMillis(10000));

        Streamer.forUUIDCache()
                .build()
                .parallel()
                .run(atomicLoan::get)
                .run(atomicLoan::get)
                .run(atomicLoan::get)
                .run(atomicLoan::get)
                .run(atomicLoan::get)
                .run(atomicLoan::get)
                .run(atomicLoan::get)
                .run(atomicLoan::get)
                .subscribe()
                .waitFor(100000);

        atomicLoan.refresh(s -> s.map(s1 -> s1 + " Krakil").orElse("WRONG"));
        atomicLoan.refresh(s -> s.map(s1 -> s1 + " Krakil").orElse("WRONG"));
        atomicLoan.refresh(s -> s.map(s1 -> s1 + " Krakil").orElse("WRONG"));

        log.info("Current {}, Undo stack {}, Redo stack {}", atomicLoan.get(), atomicLoan.undoStack(), atomicLoan.redoStack());
        atomicLoan.undo();

        log.info("Current {}, Undo stack {}, Redo stack {}", atomicLoan.get(), atomicLoan.undoStack(), atomicLoan.redoStack());
        atomicLoan.undo();

        log.info("Current {}, Undo stack {}, Redo stack {}", atomicLoan.get(), atomicLoan.undoStack(), atomicLoan.redoStack());

        Validate.assertTrue(cnt[0] == 1, "count expected to be 1 but was " + cnt[0]);
        Validate.assertTrue(atomicLoan.read().map(s -> Objects.equals(s, "Balle Krakil")).orElse(false), "Expected Balle Krakil, but was " + atomicLoan.read().orElse(""));
    }

    @Test
    void shouldWorkAsMemento() {
        MementoReference<String> memento = new MementoReference<>(10, 10);

        memento
                .set("aString")
                .set("aString")
                .set("aString");

        log.info("Current {}, Undo stack {}, Redo stack {}", memento.get(), memento.undoStack(), memento.redoStack());

        memento.undo();
        memento.undo();

        log.info("Current {}, Undo stack {}, Redo stack {}", memento.get(), memento.undoStack(), memento.redoStack());

        memento.set("bString");
        log.info("Current {}, Undo stack {}, Redo stack {}", memento.get(), memento.undoStack(), memento.redoStack());
    }


    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignore) {
        }
    }
}
