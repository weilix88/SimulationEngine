package main.java.cloud;

import main.java.aws.s3.S3FileUploader;
import main.java.azure.fileStorage.AzureFileUploader;
import main.java.config.EngineConfig;
import main.java.local.LocalFileSaver;

public class CloudFileUploaderFactory {
    public static CloudFileUploader getCloudFileUploader() {
        String platform = EngineConfig.readProperty("platform");
        if (platform.equalsIgnoreCase("aws")) {
            return new S3FileUploader(null);
        } else if (platform.equalsIgnoreCase("azure")) {
            return new AzureFileUploader();
        } else if (platform.equalsIgnoreCase("local")) {
            return new LocalFileSaver();
        }
        return null;
    }
}
