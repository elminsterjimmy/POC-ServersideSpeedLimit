package com.elminster.poc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import com.google.common.io.Files;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpeedLimitedInputStreamTest {

    private static final Logger logger = LoggerFactory.getLogger(SpeedLimitedInputStreamTest.class);

    private static final long FILE_SIZE = 9 * 1024 + 35; // 1 MB
    private static final int SPEED_LIMITION = 1 * 1024; // 1 KB/s
    static File srcFile = new File("src");
    static File destSpeedLimitedFile = new File("destspeedLimited");
    static File destSpeedUnlimitedFile = new File("destSpeedUnlimited");

    @BeforeClass
    public static void beforeTest() throws IOException {
        RandomAccessFile f = new RandomAccessFile(srcFile, "rw");
        f.setLength(FILE_SIZE);
        f.close();
    }

    @Test
    public void testSpeedLimitedInputStream() throws IOException {
        Thread speedUnlimitedThread = new Thread() {
            public void run() {
                try {
                    long now = System.currentTimeMillis();
                    testUnlimitedSpeed();
                    long elapsedTime = System.currentTimeMillis() - now;
                    logger.info("unlimited speed - elapsed time: [{} ms], speed [{} KB/s]", 
                            elapsedTime,
                            FILE_SIZE / 1024 / ((double)elapsedTime / 1000));
                } catch (IOException e) {
                    Assert.fail(e.getMessage());
                }
            }
        };

        Thread speedLimitedThread = new Thread() {
            public void run() {
                try {
                    long now = System.currentTimeMillis();
                    testLimitedSpeedWithFile();
                    long elapsedTime = System.currentTimeMillis() - now;
                    logger.info("limited speed [{} KB/s] - elapsed time: [{} ms], speed [{} KB/s]", 
                            SPEED_LIMITION / 1024, elapsedTime,
                            FILE_SIZE / 1024 / ((double)elapsedTime / 1000));
                } catch (IOException e) {
                    Assert.fail(e.getMessage());
                }
            }
        };
        

        speedLimitedThread.start();
        speedUnlimitedThread.start();
        try {
            speedLimitedThread.join();
            speedUnlimitedThread.join();
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }

        // compare files
        if (!compareFileByBytes(srcFile, destSpeedUnlimitedFile)) {
            Assert.fail(String.format("files [%s]:[%s] do NOT equal", srcFile, destSpeedUnlimitedFile));
        }
        if (!compareFileByBytes(srcFile, destSpeedLimitedFile)) {
            Assert.fail(String.format("files [%s]:[%s] do NOT equal", srcFile, destSpeedLimitedFile));
        }
    }

    private boolean compareFileByBytes(File src, File dest) throws IOException {
        return Files.equal(src, dest);
    }

    private void testUnlimitedSpeed() throws IOException {
        SpeedLimiter limter = new SpeedLimiter();
        fileCopyWithSpeedLimiter(srcFile, destSpeedUnlimitedFile, limter);        
    }

    private void fileCopyWithSpeedLimiter(File src, File dest, SpeedLimiter speedLimiter) throws IOException {
        try (FileInputStream fis = new FileInputStream(src);
                SpeedLimitedInputStream speedLimitedIn = new SpeedLimitedInputStream(fis, speedLimiter);
                FileOutputStream fout = new FileOutputStream(dest)) {
            copyStream(speedLimitedIn, fout);
        }
    }

    private void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int length;
        while ((length = in.read(buffer)) > 0) {
            out.write(buffer, 0, length);
        }
    }

    private void testLimitedSpeedWithFile() throws IOException {
        SpeedLimiter limiter = new SpeedLimiter(SPEED_LIMITION);
        fileCopyWithSpeedLimiter(srcFile, destSpeedLimitedFile, limiter);
    }

    public static void afterTest() {
        if (srcFile.exists()) {
            srcFile.delete();
        }
        if (destSpeedLimitedFile.exists()) {
            destSpeedLimitedFile.delete();
        }
        if (destSpeedUnlimitedFile.exists()) {
            destSpeedUnlimitedFile.delete();
        }
    }
}