module rx.command.main {
    requires slf4j.api;

    exports com.intact.rx.api;
    exports com.intact.rx.api.cache;
    exports com.intact.rx.api.cache.observer;
    exports com.intact.rx.api.command;
    exports com.intact.rx.api.logger;
    exports com.intact.rx.api.rxcache;
    exports com.intact.rx.api.rxcircuit;
    exports com.intact.rx.api.rxrepo;
    exports com.intact.rx.api.subject;

    exports com.intact.rx.core.cache.data.id;
    exports com.intact.rx.core.cache;
    exports com.intact.rx.core.cache.data.context;
    exports com.intact.rx.core.cache.factory;
    exports com.intact.rx.core.cache.data;

    exports com.intact.rx.exception;
    exports com.intact.rx.policy;
    exports com.intact.rx.templates;
    exports com.intact.rx.templates.annotations;
    exports com.intact.rx.templates.api;
    exports com.intact.rx.templates.key;

    exports com.intact.rx.core.rxcircuit.breaker;
    exports com.intact.rx.core.rxcircuit.rate;
    //provides RxRepository with CachedRepository;

    exports com.intact.rx.core.rxcache.acts;
    exports com.intact.rx.core.rxcache.act;
    exports com.intact.rx.core.command;
    exports com.intact.rx.core.machine.context;
    exports com.intact.rx.core.rxcache.controller;

    exports com.intact.rx.core.command.api;
    exports com.intact.rx.core.rxrepo;
    exports com.intact.rx.core.command.status;
    exports com.intact.rx.core.cache.status;
    exports com.intact.rx.core.cache.subject;
    exports com.intact.rx.core.command.observer;
    exports com.intact.rx.core.machine;
    exports com.intact.rx.core.rxcache.api;
    exports com.intact.rx.core.rxcache.factory;
    exports com.intact.rx.core.rxcache;
    exports com.intact.rx.core.machine.api;
}