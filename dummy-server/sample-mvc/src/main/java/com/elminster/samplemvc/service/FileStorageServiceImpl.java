package com.elminster.samplemvc.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final String FILE_OUTPUT_DIR = "C:/Upload/";

    @Override
    public void store(MultipartFile file) throws IOException {
        String fileName = file.getName();
        try (InputStream in = file.getInputStream();
                FileOutputStream fout = new FileOutputStream(FILE_OUTPUT_DIR + fileName)) {
            FileCopyUtils.copy(in, fout);
        }
    }

}