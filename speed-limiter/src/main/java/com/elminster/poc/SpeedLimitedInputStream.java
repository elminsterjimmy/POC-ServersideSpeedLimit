package com.elminster.poc;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpeedLimitedInputStream extends InputStream {

    private static final Logger logger = LoggerFactory.getLogger(SpeedLimitedInputStream.class);

    private static final int EOF = -1;

    private final InputStream target;
    private final SpeedLimiter speedLimter;

    public SpeedLimitedInputStream(InputStream target, SpeedLimiter speedLimiter) {
        this.target = target;
        this.speedLimter = speedLimiter;
    }

    @Override
    public int read() throws IOException {
        return target.read();
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        logger.debug("read bytes size:[{}], offset:[{}], length:[{}]", b.length, off, len);
        int sizeToRead = len;
        int read = 0;
        if (SpeedLimiter.UNLIMITED == speedLimter.getMaxSpeedInBytesPerSec()) { // unlimted
            logger.debug("Unlimited Speed.");
            read = target.read(b, off, len);
        } else {
            while (sizeToRead > 0) {
                logger.debug("Limit Speed to {} bytes/sec.", speedLimter.getMaxSpeedInBytesPerSec());
                int remainBytesToRead = speedLimter.getBytesRemains();
                logger.debug("trying to read bytes. limiter remain bytes [{}]", remainBytesToRead);
                int curRead = target.read(b, off, sizeToRead);
                if (EOF == curRead) {
                    break;
                }
                read += curRead;
                speedLimter.setBytesRemains(remainBytesToRead - curRead);
                off += curRead;
                sizeToRead -= curRead;
                if (remainBytesToRead >= sizeToRead) {
                    logger.debug("do NOT hit the max bytes limition, read [{}] bytes", sizeToRead);
                } else {
                    logger.debug("hit the max limition, read part of the bytes. actual bytes lenth:[{}], read bytes length:[{}], offset:[{}]",
                            len, remainBytesToRead, off);
                    try {
                        logger.debug("waiting for next read");
                        Thread.sleep(SpeedLimiter.SPEED_RATE);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        logger.error(e.getMessage(), e);
                    }
                }
                logger.debug("read bytes.");
            }   
        }
        return read;
    }

    @Override
    public void close() throws IOException {
        speedLimter.close();
        super.close();
    }
}