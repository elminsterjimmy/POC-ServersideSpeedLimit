package com.elminster.samplemvc.service;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    public void store(MultipartFile file) throws IOException;
}