package com.connectsphere.media_service.service;

// Value object returned after a file is written to storage.
public record StoredMediaFile(String storedFilename, String publicUrl) {
}
