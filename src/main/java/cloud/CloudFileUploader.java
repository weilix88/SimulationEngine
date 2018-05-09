package main.java.cloud;

import com.google.gson.JsonObject;

import java.io.File;

public interface CloudFileUploader {
    JsonObject createFolder(String bucketName, String folder);
    JsonObject upload(String bucketName, String path, File file, String fileName);
}
