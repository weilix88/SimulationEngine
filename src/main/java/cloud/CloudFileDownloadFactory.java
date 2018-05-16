package main.java.cloud;

import main.java.aws.s3.S3FileDownloader;
import main.java.azure.fileStorage.AzureFileDownloader;
import main.java.config.EngineConfig;
import main.java.local.LocalFileDownloader;

public class CloudFileDownloadFactory {
	public static CloudFileDownloader getCloudFileDownloader() {
		String platform = EngineConfig.readProperty("platform");
    	if(platform.equalsIgnoreCase("aws")) {
    		return new S3FileDownloader(null);
    	}else if(platform.equalsIgnoreCase("azure")) {
    		return new AzureFileDownloader();
    	}else if(platform.equalsIgnoreCase("local")){
    		return new LocalFileDownloader();
		}
    	return null;
	}
}
