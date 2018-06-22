package com.intact.rx.monitor;

import java.lang.ref.WeakReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.core.cache.data.DataCache;
import com.intact.rx.core.cache.data.ObjectRoot;
import com.intact.rx.core.rxcache.api.Act;
import com.intact.rx.core.rxcache.api.ActsController;
import com.intact.rx.core.rxcache.factory.RxCacheFactory;

final class RxCacheMonitorAlgorithms {

    private static final Logger log = LoggerFactory.getLogger(RxCacheMonitorAlgorithms.class);

    static void monitorActs() {

        DataCache<Object, WeakReference<Act<Object, Object>>> monitorCache = RxCacheFactory.actMonitorCache();

        int recentlyGarbageCollected = 0;
        for (ObjectRoot<Object, WeakReference<Act<Object, Object>>> objectRoot : monitorCache.getRoots()) {
            WeakReference<Act<Object, Object>> reference = objectRoot.getValueNoStatusUpdate();
            if (reference == null) {
                log.warn("Weak reference to Act was null!");
                objectRoot.setExpired();
            } else {
                Act<Object, Object> act = reference.get();
                if (act == null) {
                    objectRoot.setExpired();
                    ++recentlyGarbageCollected;
                }
            }
        }

        if (monitorCache.size() > 1000 || recentlyGarbageCollected > 50) {
            log.info("# of Acts cached is {}", monitorCache.size());
            log.info("# of Acts recently garbage collected {}", recentlyGarbageCollected);
        }
    }

    static void monitorActsControllers() {
        DataCache<Object, WeakReference<ActsController>> monitorCache = RxCacheFactory.actsControllerMonitorCache();

        int recentlyGarbageCollected = 0;
        for (ObjectRoot<Object, WeakReference<ActsController>> objectRoot : monitorCache.getRoots()) {
            WeakReference<ActsController> reference = objectRoot.getValueNoStatusUpdate();
            if (reference == null) {
                log.warn("Weak reference to ActsController was null!");
                objectRoot.setExpired();
            } else {
                ActsController controller = reference.get();
                if (controller == null) {
                    objectRoot.setExpired();
                    ++recentlyGarbageCollected;
                }
            }
        }

        if (monitorCache.size() > 1000 || recentlyGarbageCollected > 50) {
            log.info("# of ActsControllers cached is {}", monitorCache.size());
            log.info("# of ActsControllers recently garbage collected {}", recentlyGarbageCollected);
        }
    }

    private RxCacheMonitorAlgorithms() {
    }
}
