package com.intact.rx.api;

import com.intact.rx.monitor.CacheMonitor;
import com.intact.rx.monitor.CommandMonitor;
import com.intact.rx.monitor.MachineMonitor;
import com.intact.rx.monitor.RxCacheMonitor;

public final class RxRuntime {

    public static boolean initialize() {

        //start rx monitors
        CacheMonitor cacheMonitor = CacheMonitor.instance();
        CommandMonitor commandMonitor = CommandMonitor.instance();
        RxCacheMonitor rxCacheMonitor = RxCacheMonitor.instance();
        MachineMonitor machineMonitor = MachineMonitor.instance();

        return cacheMonitor.isInitialized() &&
                commandMonitor.isInitialized() &&
                rxCacheMonitor.isInitialized() &&
                machineMonitor.isInitialized();
    }

    private RxRuntime() {
    }
}
