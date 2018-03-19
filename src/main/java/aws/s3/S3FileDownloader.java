package main.java.aws.s3;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.model.S3Object;

import main.java.aws.meta.SDKEndPoint;
import main.java.cloud.CloudFileDownloader;
import main.java.config.EngineConfig;
import main.java.util.FileUtil;

public class S3FileDownloader extends SDKEndPoint implements CloudFileDownloader {
    public S3FileDownloader(Regions r) {
        super(r);
    }
    
    private File downloadFile(String bucketName, String pathToFolder, String fileName) {
    	File res = FileUtil.createTempFile(fileName);
        byte[] buf = new byte[4096];
        int count = -1;
        try{
            S3Object s3Obj = s3client.getObject(bucketName, pathToFolder+"/"+fileName);
            if(s3Obj==null){
                return null;
            }
            
            try(InputStream is = s3Obj.getObjectContent();
                    OutputStream os = new FileOutputStream(res)){
                while((count=is.read(buf)) != -1){
                    os.write(buf, 0, count);
                }
            }
        }catch (IOException e){};
        return res;
    }
    
    @Override
    public File downloadCustomeWeatherFile(String pathToFolder, String fileName){
    	String bucketName = EngineConfig.readProperty("CustomWeatherFileCloud");
        return downloadFile(bucketName, pathToFolder, fileName);
    }
    
    @Override
    public File downloadWeatherFile(String pathToFolder, String fileName){
    	String bucketName = EngineConfig.readProperty("WeatherFileS3");
        return downloadFile(bucketName, pathToFolder, fileName);
    }
    
    @Override
    public File downloadScheduleFile(String bucketName, String pathToFolder, String fileName){
    	return downloadFile(bucketName, pathToFolder, fileName);
    }
    
    public static void main(String[] args){}
}
