package com.intact.rx.core.cache.status;

@SuppressWarnings("SynchronizedMethod")
public class AccessStatus {
    private final AccessCount count;
    private final AccessTime time;
    private AccessState state;

    public enum AccessState {
        READ,
        WRITE,
        MODIFIED,
        NOT_MODIFIED,
        REMOVED,
        EXPIRED
    }

    public AccessStatus() {
        this.count = new AccessCount();
        this.time = new AccessTime();
        this.state = AccessState.WRITE;
    }

    public AccessStatus(AccessStatus status) {
        this.count = status.count.copy();
        this.time = status.time.copy();
        this.state = status.state;
    }

    public AccessStatus copy() {
        return new AccessStatus(this);
    }

    public synchronized void modified() {
        count.modified();
        time.modified();
        state = AccessState.MODIFIED;
    }

    public synchronized void read() {
        count.read();
        time.read();
        state = AccessState.READ;
    }

    public synchronized void notModified() {
        count.notModified();
        time.notModified();
        state = AccessState.NOT_MODIFIED;
    }

    public synchronized void expired() {
        state = AccessState.EXPIRED;
    }

    public synchronized void removed() {
        state = AccessState.REMOVED;
    }

    public synchronized void renewLoan() {
        time.start();
    }

    public synchronized AccessTime getTime() {
        return time;
    }

    public synchronized AccessCount getCount() {
        return count;
    }

    public synchronized boolean isRead() {
        return state == AccessState.READ;
    }

    public synchronized boolean isModified() {
        return state == AccessState.MODIFIED;
    }

    public synchronized boolean isCreated() {
        return state == AccessState.WRITE;
    }

    public synchronized boolean isExpired() {
        return state == AccessState.EXPIRED;
    }

    public synchronized AccessState getAccessState() {
        return state;
    }

    @Override
    public synchronized String toString() {
        return "AccessStatus{" +
                "count=" + count +
                ", time=" + time +
                ", state=" + state +
                '}';
    }
}
