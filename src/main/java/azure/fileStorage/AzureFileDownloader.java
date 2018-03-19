package main.java.azure.fileStorage;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.file.CloudFile;
import com.microsoft.azure.storage.file.CloudFileClient;
import com.microsoft.azure.storage.file.CloudFileDirectory;
import com.microsoft.azure.storage.file.CloudFileShare;

import main.java.cloud.CloudFileDownloader;
import main.java.config.EngineConfig;
import main.java.util.FileUtil;

public class AzureFileDownloader implements CloudFileDownloader {
	private final Logger LOG = LoggerFactory.getLogger(this.getClass());
	
	private File download(CloudFile cloudFile, String fileName) {
		File res = FileUtil.createTempFile(fileName);
		try {
			cloudFile.downloadToFile(res.getAbsolutePath());
		} catch (StorageException | IOException e) {
			LOG.error("Download failed: "+cloudFile.getUri().getPath()+", "+fileName+", "+e.getMessage(), e);
			return null;
		}
		
		return res;
	}
	
	private CloudFile getCloudFile(String share, String path, String fileName) {
		CloudFileDirectory dir = getDirCreateIfNotExist(share, path);
	    
	    CloudFile cloudFile = null;
	    try {
			cloudFile = dir.getFileReference(fileName);
		} catch (URISyntaxException | StorageException e) {
			LOG.error("Get cloud file failed: "+share+", "+path+", "+fileName+", "+e.getMessage(), e);
		}
	    
	    return cloudFile;
	}
	
	private CloudFileDirectory getDirCreateIfNotExist(String share, String path) {
		CloudFileDirectory dir = getDirOnly(share, path);
		
		if(dir!=null) {
			try {
				dir.createIfNotExists();
			} catch (StorageException | URISyntaxException e) {
				LOG.error("Dir create if not exist failed: "+share+", "+path+", "+e.getMessage(), e);
				return null;
			}
		}
		
	    return dir;
	}
	
	private CloudFileDirectory getDirOnly(String share, String path) {
		CloudFileClient fileClient = getFileClient();
		if(fileClient==null) {
			return null;
		}
		
		CloudFileShare fileShare = null;
		try {
			fileShare = fileClient.getShareReference(share);
		} catch (URISyntaxException | StorageException e) {			
			LOG.error("Cannot get file share "+share+", "+e.getMessage(), e);
			return null;
		}
		
		try {
			fileShare.createIfNotExists();
		} catch (StorageException e) {
			LOG.error("Share create if not exist failed: "+share+", "+e.getMessage(), e);
			return null;
		}
		
		CloudFileDirectory rootDir = null;
		try {
			rootDir = fileShare.getRootDirectoryReference();
		} catch (StorageException | URISyntaxException e) {
			LOG.error("Get root directory failed: "+share+", "+e.getMessage(), e);
			return null;
		}

		if(path==null) {
			return rootDir;
		}
		
		CloudFileDirectory dir = null;
	    try {
			dir = rootDir.getDirectoryReference(path);
		} catch (URISyntaxException | StorageException e) {
			LOG.error("Get directory failed: "+share+", "+path+", "+e.getMessage(), e);
			return null;
		}
	    
	    return dir;
	}
	
	private CloudFileClient getFileClient() {
		String storageConnectionString =
			    "DefaultEndpointsProtocol=https;"
			    + "AccountName="+EngineConfig.readProperty("AzureFileStorageAccountName")+";"
			    + "AccountKey="+EngineConfig.readProperty("AzureFileStorageAccountKey")+";"
			    + "EndpointSuffix=core.windows.net";
		
		try {
			CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
			
			return storageAccount.createCloudFileClient();
		} catch (InvalidKeyException | URISyntaxException e) {
			LOG.error("Cannot get Azure file client, "+e.getMessage(), e);
		}
		
		return null;
	}
	
	@Override
    public File downloadCustomeWeatherFile(String pathToFolder, String fileName){
		String share = EngineConfig.readProperty("CustomWeatherFileCloud");
		CloudFile cloudFile = getCloudFile(share, pathToFolder, fileName);
		if(cloudFile==null) {
			return null;
		}
		
		return download(cloudFile, fileName);
	}
	
	@Override
    public File downloadWeatherFile(String pathToFolder, String fileName){
		String share = EngineConfig.readProperty("WeatherFileAzure");
		CloudFile cloudFile = getCloudFile(share, pathToFolder, fileName);
		if(cloudFile==null) {
			return null;
		}
		
		return download(cloudFile, fileName);
	}
	
	@Override
    public File downloadScheduleFile(String bucketName, String pathToFolder, String fileName){
    	String share = EngineConfig.readProperty("ProjectsRepositoryAzure");
    	
    	CloudFile cloudFile = getCloudFile(share, bucketName+"/"+pathToFolder, fileName);
		if(cloudFile==null) {
			return null;
		}
		
		return download(cloudFile, fileName);
    }
}
