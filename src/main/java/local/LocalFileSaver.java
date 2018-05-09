package main.java.local;

import com.google.gson.JsonObject;
import main.java.cloud.CloudFileUploader;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class LocalFileSaver implements CloudFileUploader {
    @Override
    public JsonObject createFolder(String bucketName, String folder) {
        File file = new File("D:\\IDFSimResultSaves\\"+folder);
        file.mkdirs();

        JsonObject ret = new JsonObject();
        ret.addProperty("status", "success");
        return ret;
    }

    @Override
    public JsonObject upload(String bucketName, String path, File file, String fileName) {
        File dest = new File("D:\\IDFSimResultSaves\\"+path+"\\"+fileName);

        JsonObject ret = new JsonObject();
        try {
            FileUtils.copyFile(file, dest);
            ret.addProperty("status", "success");
        } catch (IOException e) {
            e.printStackTrace();
            ret.addProperty("status", "error");
            ret.addProperty("error_msg", e.getMessage());
        }

        return ret;
    }
}
