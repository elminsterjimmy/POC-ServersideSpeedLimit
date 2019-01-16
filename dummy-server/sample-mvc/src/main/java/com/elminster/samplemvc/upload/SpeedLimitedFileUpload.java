package com.elminster.samplemvc.upload;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.elminster.poc.SpeedLimitedInputStream;
import com.elminster.poc.SpeedLimiter;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpeedLimitedFileUpload extends ServletFileUpload {

    private static final int SPEED_LIMIT = 1024 * 1024; // 1MB

    private static final Logger logger = LoggerFactory.getLogger(SpeedLimitedFileUpload.class);

    public SpeedLimitedFileUpload() {
        super();
    }

    public SpeedLimitedFileUpload(FileItemFactory fileItemFactory) {
        super(fileItemFactory);
    }

    public List<FileItem> parseRequest(RequestContext ctx)
            throws FileUploadException {
        List<FileItem> items = new ArrayList<>();
        boolean successful = false;
        try {
            FileItemIterator iter = getItemIterator(ctx);
            FileItemFactory fac = getFileItemFactory();
            if (fac == null) {
                throw new NullPointerException("No FileItemFactory has been set.");
            }
            while (iter.hasNext()) {
                final FileItemStream item = iter.next();
                final String fileName = getFileName(item.getHeaders());
                FileItem fileItem = fac.createItem(item.getFieldName(), item.getContentType(),
                                                   item.isFormField(), fileName);
                items.add(fileItem);
                SpeedLimitedInputStream speedLimitedIn = null;
                try {
                    InputStream in = item.openStream();
                    SpeedLimiter limiter = new SpeedLimiter(SPEED_LIMIT);
                    speedLimitedIn = new SpeedLimitedInputStream(in, limiter);
                    logger.info("start saving file [{}]...", fileName);
                    logger.info("limit the file upload to [{}]", 
                        SpeedLimiter.UNLIMITED == limiter.getMaxSpeedInBytesPerSec() ? 
                        "Unlimited" : limiter.getMaxSpeedInBytesPerSec() + " KB/s");
                    long now = System.currentTimeMillis();
                    long size = Streams.copy(speedLimitedIn, fileItem.getOutputStream(), true);
                    long elasped = System.currentTimeMillis() - now;
                    logger.info("saving file finished, elasped time: [{} ms], speed [{} KB/s]",
                        elasped, size / 1024 / ((double)elasped / 1000));
                } catch (FileUploadIOException e) {
                    throw (FileUploadException) e.getCause();
                } catch (IOException e) {
                    throw new IOFileUploadException(String.format("Processing of %s request failed. %s",
                                                           MULTIPART_FORM_DATA, e.getMessage()), e);
                } finally {
                    if (null != speedLimitedIn) {
                        speedLimitedIn.close();
                    }
                }
                final FileItemHeaders fih = item.getHeaders();
                fileItem.setHeaders(fih);
            }
            successful = true;
            return items;
        } catch (FileUploadIOException e) {
            throw (FileUploadException) e.getCause();
        } catch (IOException e) {
            throw new FileUploadException(e.getMessage(), e);
        } finally {
            if (!successful) {
                for (FileItem fileItem : items) {
                    try {
                        fileItem.delete();
                    } catch (Exception ignored) {
                        // ignored TODO perhaps add to tracker delete failure list somehow?
                    }
                }
            }
        }
    }
}