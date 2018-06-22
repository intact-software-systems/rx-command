package com.intact.rx.templates;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intact.rx.templates.api.InitializeMethods;

/**
 * Implements variations of the "Double checked locking pattern".
 */
public final class DoubleCheckedLocking {
    private static final Logger log = LoggerFactory.getLogger(DoubleCheckedLocking.class);

    // --------------------------------------------------
    // Initializes once using "Double checked locking pattern"
    // --------------------------------------------------
    public static boolean initialize(InitializeMethods service) {
        if (service == null) {
            log.error("Object given to initialize function is null!");
            return false;
        }

        if (service.isInitialized()) {
            return true;
        }

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (service) {
            if (service.isInitialized()) {
                return true;
            }

            service.initialize();

            return service.isInitialized();
        }
    }

    private DoubleCheckedLocking() {
    }
}
