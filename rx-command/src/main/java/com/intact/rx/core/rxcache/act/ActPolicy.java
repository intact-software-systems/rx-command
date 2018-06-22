package com.intact.rx.core.rxcache.act;

import static java.util.Objects.requireNonNull;

import com.intact.rx.api.cache.CachePolicy;
import com.intact.rx.core.cache.data.context.CacheMasterPolicy;
import com.intact.rx.core.cache.data.context.DataCachePolicy;
import com.intact.rx.core.command.CommandControllerPolicy;
import com.intact.rx.policy.*;

@SuppressWarnings("WeakerAccess")
public final class ActPolicy {
    private static final ActPolicy noReloadAndTwentyThousandSamples = noReload(CachePolicy.create(CacheMasterPolicy.validForever(), DataCachePolicy.leastRecentlyUsedAnd(ResourceLimits.maxSamplesSoft(20000), Lifetime.forever())));

    private final CommandControllerPolicy commandControllerPolicy;
    private final CachePolicy cachePolicy;

    private final Reload reload;
    private final Attempt attempt;
    private final Lifetime lifetime;
    private final Extension extension;

    public ActPolicy(
            CommandControllerPolicy commandControllerPolicy,
            CachePolicy cachePolicy,
            Reload reload,
            Attempt attempt,
            Lifetime lifetime,
            Extension extension) {
        this.commandControllerPolicy = requireNonNull(commandControllerPolicy);
        this.cachePolicy = requireNonNull(cachePolicy);
        this.reload = requireNonNull(reload);
        this.attempt = requireNonNull(attempt);
        this.lifetime = requireNonNull(lifetime);
        this.extension = requireNonNull(extension);
    }

    public Reload getReload() {
        return reload;
    }

    public Attempt getAttempt() {
        return attempt;
    }

    public Lifetime getLifetime() {
        return lifetime;
    }

    public Extension getExtension() {
        return extension;
    }

    public CommandControllerPolicy getCommandControllerPolicy() {
        return commandControllerPolicy;
    }

    public CachePolicy getCachePolicy() {
        return cachePolicy;
    }

    // --------------------------------------------
    // Convenience factories
    // --------------------------------------------

    public static ActPolicy from(ActPolicy actPolicy, CachePolicy cachePolicy) {
        return new ActPolicy(
                actPolicy.commandControllerPolicy,
                cachePolicy,
                actPolicy.reload,
                actPolicy.attempt,
                actPolicy.lifetime,
                actPolicy.extension
        );
    }

    public static ActPolicy noReload(CachePolicy cachePolicy) {
        return new ActPolicy(CommandControllerPolicy.parallel(), cachePolicy, Reload.no(), Attempt.once(), cachePolicy.getDataCachePolicy().getLifetime(), Extension.noRenew());
    }

    public static ActPolicy reloadOnDelete(CachePolicy cachePolicy) {
        return new ActPolicy(CommandControllerPolicy.parallel(), cachePolicy, Reload.onDelete(), Attempt.forever(), cachePolicy.getDataCachePolicy().getLifetime(), Extension.noRenew());
    }

    public static ActPolicy reloadOnModify(CachePolicy cachePolicy) {
        return new ActPolicy(CommandControllerPolicy.parallel(), cachePolicy, Reload.onModify(), Attempt.forever(), cachePolicy.getDataCachePolicy().getLifetime(), Extension.noRenew());
    }

    public static ActPolicy noReloadAndTwentyThousandSamples() {
        return noReloadAndTwentyThousandSamples;
    }

    @Override
    public String toString() {
        return "ActPolicy{" +
                "reload=" + reload +
                ", commandControllerPolicy=" + commandControllerPolicy +
                ", cachePolicy=" + cachePolicy +
                '}';
    }
}
