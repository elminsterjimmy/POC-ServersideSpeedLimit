package com.elminster.poc;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SpeedLimiter implements Closeable {
    public static final Integer UNLIMITED = -1;
    private static final Integer DEFAULT_LIMITION = UNLIMITED;
    private static final int INTERVAL = 1000; // 1 sec
    public static final int SPEED_RATE = 200; // 200 ms per tick
    private static final int UPDATE_TIME = INTERVAL / SPEED_RATE;
    
    private final Refresher refresher = new Refresher();

    private final Integer maxSpeedInBytesPerSec;

    private Integer bytesRemains;

    private volatile boolean stop = false;

    private ReadWriteLock lock = new ReentrantReadWriteLock();

    public SpeedLimiter() {
        this(DEFAULT_LIMITION);
    }

    public SpeedLimiter(Integer maxSpeedInBytesPerSec) {
        if (maxSpeedInBytesPerSec <= 0 && UNLIMITED != maxSpeedInBytesPerSec) {
            throw new IllegalArgumentException("Max Speed in Bytes per Second should be greater than 0.");
        }
        this.maxSpeedInBytesPerSec = maxSpeedInBytesPerSec;
        refresher.start();
    }

    class Refresher extends Thread {
        public void run() {
            while (!stop) {
                setBytesRemains(maxSpeedInBytesPerSec / UPDATE_TIME);
                try {
                    Thread.sleep(SPEED_RATE);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    public Integer getMaxSpeedInBytesPerSec() {
        return maxSpeedInBytesPerSec;
    }

    public Integer getBytesRemains() {
        Lock rLock = lock.readLock();
        try {
            rLock.lock();
            return bytesRemains;
        } finally {
            rLock.unlock();
        }
    }

    public void setBytesRemains(Integer bytesRemains) {
        Lock wLock = lock.writeLock();
        try {
            wLock.lock();
            this.bytesRemains = bytesRemains;
        } finally {
            wLock.unlock();
        }
    }

    @Override
    public void close() throws IOException {
        this.stop = true;
	}
}