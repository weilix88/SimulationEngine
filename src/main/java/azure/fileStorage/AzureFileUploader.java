package main.java.azure.fileStorage;

import com.google.gson.JsonObject;
import main.java.cloud.CloudFileUploader;
import main.java.cloud.GlobalConstant;
import main.java.config.EngineConfig;

import java.io.File;

public class AzureFileUploader implements CloudFileUploader {
    @Override
    public JsonObject createFolder(String bucketName, String folder) {
        String share = EngineConfig.readProperty("ProjectsRepositoryAzure");
        AzureFileUtil.createDir(share, bucketName + "/" + folder);

        JsonObject ret = new JsonObject();
        ret.addProperty("status", "success");
        return ret;
    }

    @Override
    public JsonObject upload(String bucketName, String path, File file, String fileName) {
        JsonObject ret = new JsonObject();

        if (file != null) {
            if(AzureFileUtil.uploadFile(bucketName, path, file, fileName)){
                ret.addProperty("status", "success");
            }else {
                ret.addProperty("status", "error");
                ret.addProperty("error_msg", "Upload file failed");
            }
        }else {
            ret.addProperty("status", "error");
            ret.addProperty("error_msg", "Upload file is null");
        }

        return ret;
    }
}
