package com.intact.rx.core.cache.status;

@SuppressWarnings({"SynchronizedMethod", "unused"})
public class AccessCount {
    private long readCount;
    private long modifiedCount;

    public AccessCount() {
    }

    public AccessCount(AccessCount accessCount) {
        this.readCount = accessCount.readCount;
        this.modifiedCount = accessCount.modifiedCount;
    }

    public AccessCount copy() {
        return new AccessCount(this);
    }

    public synchronized void modified() {
        ++modifiedCount;
    }

    public void notModified() {
        // Currently no access count for not modified
    }

    public synchronized void read() {
        ++readCount;
    }

    public synchronized long getReadCount() {
        return readCount;
    }

    public synchronized long getModifiedCount() {
        return modifiedCount;
    }

    public synchronized long getTotalCount() {
        return readCount + modifiedCount;
    }

    @Override
    public synchronized String toString() {
        return "AccessCount{" +
                "readCount=" + readCount +
                ", modifiedCount=" + modifiedCount +
                '}';
    }
}
