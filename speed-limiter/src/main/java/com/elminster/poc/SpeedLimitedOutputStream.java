package com.elminster.poc;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SpeedLimitedOutputStream extends OutputStream {
    private static final Logger logger = LoggerFactory.getLogger(SpeedLimitedOutputStream.class);

    private final OutputStream target;
    private final SpeedLimiter speedLimter;

   public SpeedLimitedOutputStream(OutputStream target, SpeedLimiter speedLimter) {
       this.target = target;
       this.speedLimter = speedLimter;
       if (null == target || null == speedLimter) {
           throw new IllegalArgumentException("Target OutputStream and Speed Limiter can NOT be null.");
       }
    }

    @Override
    public void write(int b) throws IOException {
        target.write(b);
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
        logger.debug("wrtie bytes size:[{}], offset:[{}], length:[{}]", b.length, off, len);
        int sizeToWrite = len;
        if (SpeedLimiter.UNLIMITED == speedLimter.getMaxSpeedInBytesPerSec()) { // unlimted
            logger.debug("Unlimited Speed.");
            target.write(b, off, len);
        } else {
            while (sizeToWrite > 0) {
                logger.debug("Limit Speed to {} bytes/sec.", speedLimter.getMaxSpeedInBytesPerSec());
                int remainBytesToWrite = speedLimter.getBytesRemains();
                logger.debug("trying to write bytes. limiter remain bytes [{}]", remainBytesToWrite);
                if (remainBytesToWrite >= sizeToWrite) {
                    logger.debug("do NOT hit the max bytes limition, write [{}] bytes", sizeToWrite);
                    target.write(b, off, sizeToWrite);
                    off += sizeToWrite;
                    sizeToWrite -= sizeToWrite;
                    speedLimter.setBytesRemains(remainBytesToWrite - sizeToWrite);
                } else {
                    logger.debug("hit the max limition, write part of the bytes. actual bytes lenth:[{}], write bytes length:[{}]",
                            len, remainBytesToWrite);
                    target.write(b, off, remainBytesToWrite);
                    off += remainBytesToWrite;
                    sizeToWrite -= remainBytesToWrite;
                    speedLimter.setBytesRemains(0);
                    try {
                        logger.debug("waiting for next wrtie");
                        Thread.sleep(SpeedLimiter.SPEED_RATE);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        logger.error(e.getMessage(), e);
                    }
                }
                logger.debug("written bytes.");
            }   
        }
    }

    @Override
    public void close() throws IOException {
        speedLimter.close();
        super.close();
    }
}