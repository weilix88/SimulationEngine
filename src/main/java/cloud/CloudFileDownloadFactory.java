package main.java.cloud;

import main.java.aws.s3.S3FileDownloader;
import main.java.azure.fileStorage.AzureFileDownloader;
import main.java.config.EngineConfig;

public class CloudFileDownloadFactory {
	public static CloudFileDownloader getCloudFileDownloader() {
		String platform = EngineConfig.readProperty("platform");
    	if(platform.equalsIgnoreCase("aws")) {
    		return new S3FileDownloader(null);
    	}else if(platform.equalsIgnoreCase("azure")) {
    		return new AzureFileDownloader();
    	}
    	return null;
	}
}
