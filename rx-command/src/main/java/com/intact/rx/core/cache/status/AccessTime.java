package com.intact.rx.core.cache.status;

@SuppressWarnings({"SynchronizedMethod", "unused", "WeakerAccess"})
public class AccessTime {
    private final long createdTime;
    private long startTime;
    private long modifiedTime;
    private long readTime;

    public AccessTime() {
        this.createdTime = System.currentTimeMillis();
        this.startTime = createdTime;
        this.modifiedTime = createdTime;
        this.readTime = createdTime;
    }

    public AccessTime(AccessTime accessTime) {
        this.createdTime = accessTime.createdTime;
        this.startTime = accessTime.startTime;
        this.modifiedTime = accessTime.modifiedTime;
        this.readTime = accessTime.readTime;
    }

    public AccessTime copy() {
        return new AccessTime(this);
    }

    public synchronized void modified() {
        modifiedTime = System.currentTimeMillis();
    }

    public synchronized void notModified() {
        // Currently no access time for not modified
    }

    public synchronized void read() {
        readTime = System.currentTimeMillis();
    }

    public synchronized void start() {
        startTime = System.currentTimeMillis();
    }
    
    public synchronized long getCreatedTime() {
        return createdTime;
    }

    public synchronized long getStartTime() {
        return startTime;
    }

    public synchronized long getModifiedTime() {
        return modifiedTime;
    }

    public synchronized long getReadTime() {
        return readTime;
    }

    public synchronized long getWriteTime() {
        return Math.max(createdTime, modifiedTime);
    }

    public synchronized long getLatestAccessTime() {
        return Math.max(modifiedTime, readTime);
    }

    public synchronized long getTimeSinceCreated() {
        return System.currentTimeMillis() - createdTime;
    }

    public synchronized long getTimeSinceStarted() {
        return System.currentTimeMillis() - startTime;
    }

    public synchronized long getTimeSinceModified() {
        return System.currentTimeMillis() - modifiedTime;
    }

    public synchronized long getTimeSinceRead() {
        return System.currentTimeMillis() - readTime;
    }

    public synchronized long getTimeSinceAccessed() {
        long currMs = System.currentTimeMillis();
        return Math.max(currMs - modifiedTime, currMs - readTime);
    }

    @Override
    public synchronized String toString() {
        return "AccessTime{" +
                "createdTime=" + createdTime +
                ", modifiedTime=" + modifiedTime +
                ", readTime=" + readTime +
                '}';
    }
}
