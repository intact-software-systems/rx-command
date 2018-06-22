package com.intact.rx.core.cache.data;

import com.intact.rx.core.cache.status.AccessStatus;
import com.intact.rx.policy.Extension;

final class CacheStatusUpdateAlgorithms {
    static void processOnRead(AccessStatus accessStatus, Extension extension) {
        accessStatus.read();

        if (extension.isRenewOnRead() || extension.isRenewOnAccess()) {
            accessStatus.renewLoan();
        }
    }

    static void processOnModified(AccessStatus accessStatus, Extension extension) {
        accessStatus.modified();

        if (extension.isRenewOnWrite() || extension.isRenewOnAccess()) {
            accessStatus.renewLoan();
        }
    }

    private CacheStatusUpdateAlgorithms() {
    }
}
