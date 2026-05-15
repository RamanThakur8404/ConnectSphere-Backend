package com.connectsphere.media_service.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface MediaStorageService {

    StoredMediaFile store(MultipartFile file);

    Resource loadAsResource(String storedFilename);

    String resolveContentType(String storedFilename);

    void delete(String storedFilename);
}
