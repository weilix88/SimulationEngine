package main.java.cloud;

import java.io.File;

public interface CloudFileDownloader {
	File downloadWeatherFile(String pathToFolder, String fileName);
	File downloadCustomeWeatherFile(String pathToFolder, String fileName);
	File downloadScheduleFile(String bucketName, String pathToFolder, String fileName);
}
