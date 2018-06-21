package main.java.local;

import main.java.cloud.CloudFileDownloader;
import main.java.config.EngineConfig;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class LocalFileDownloader implements CloudFileDownloader {
    @Override
    public File downloadWeatherFile(String pathToFolder, String fileName) {
        URI uri = null;
        try {
            uri = new URI("file:///" + EngineConfig.readProperty("WeatherFilesBasePath") + pathToFolder + "/" + fileName);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return new File(uri);
    }

    @Override
    public File downloadCustomeWeatherFile(String pathToFolder, String fileName) {
        return new File("D:\\IDFSimResultSaves\\"+pathToFolder+"\\"+fileName);
    }

    @Override
    public File downloadScheduleFile(String bucketName, String pathToFolder, String fileName) {
        return null;
    }

    @Override
    public File downloadCustomeScheduleCSVFile(String pathToFolder, String zipFileName) {
        return new File("D:\\IDFSimResultSaves\\"+pathToFolder+"\\"+zipFileName);
    }
}
