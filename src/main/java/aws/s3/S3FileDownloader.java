package main.java.aws.s3;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.model.S3Object;

import main.java.aws.meta.SDKEndPoint;
import main.java.util.FileUtil;

public class S3FileDownloader extends SDKEndPoint {
    public S3FileDownloader(Regions r) {
        super(r);
    }
    
    public File download(String bucketName, String path, String fileName){
        File res = FileUtil.createTempFile(fileName);
        byte[] buf = new byte[4096];
        int count = -1;
        try{
            S3Object s3Obj = s3client.getObject(bucketName, path+fileName);
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
    
    public static void main(String[] args){}
}
