package com.intact.rx.policy;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.intact.rx.api.FutureStatus;

public class FaultPolicy {
    private static final FaultPolicy neverThrowWhenOptional = new FaultPolicy(FutureStatus.values());
    private static final FaultPolicy throwAllErrorsExceptTimeoutWhenOptional = new FaultPolicy(new FutureStatus[]{FutureStatus.Timedout, FutureStatus.Success});
    private static final FaultPolicy throwAllErrorsWhenOptional = new FaultPolicy(new FutureStatus[]{FutureStatus.Success});

    private final Collection<FutureStatus> ignoreFailures;

    private FaultPolicy(FutureStatus[] ignoreFailures) {
        this.ignoreFailures = Stream.of(ignoreFailures).collect(Collectors.toCollection(HashSet::new));
    }

    public boolean isIgnored(FutureStatus... futureStatus) {
        return Stream.of(futureStatus).map(ignoreFailures::contains).findFirst().orElse(false);
    }

    public static FaultPolicy neverThrowWhenOptional() {
        return neverThrowWhenOptional;
    }

    public static FaultPolicy throwAllExceptTimeoutWhenOptional() {
        return throwAllErrorsExceptTimeoutWhenOptional;
    }

    public static FaultPolicy throwAllWhenOptional() {
        return throwAllErrorsWhenOptional;
    }

    @Override
    public String toString() {
        return "FaultPolicy{" +
                "ignoreFailures=" + ignoreFailures +
                '}';
    }

    @SuppressWarnings("ControlFlowStatementWithoutBraces")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FaultPolicy)) return false;
        FaultPolicy that = (FaultPolicy) o;
        return Objects.equals(ignoreFailures, that.ignoreFailures);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ignoreFailures);
    }
}
