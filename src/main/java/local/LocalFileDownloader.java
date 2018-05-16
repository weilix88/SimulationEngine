package main.java.local;

import main.java.cloud.CloudFileDownloader;

import java.io.File;

public class LocalFileDownloader implements CloudFileDownloader {
    @Override
    public File downloadWeatherFile(String pathToFolder, String fileName) {
        return null;
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
