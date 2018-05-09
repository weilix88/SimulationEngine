package main.java.aws.s3;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.gson.JsonObject;
import main.java.aws.meta.SDKEndPoint;
import main.java.cloud.CloudFileUploader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

public class S3FileUploader extends SDKEndPoint implements CloudFileUploader {
    public S3FileUploader(Regions r) {
        super(r);
    }

    public static void main(String[] args) {
    }

    @Override
    public JsonObject createFolder(String bucketName, String folder) {
        boolean isFolderExisted = s3client.doesObjectExist(bucketName, folder);
        if (!isFolderExisted) {
            //createFolder
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(0);

            InputStream emptyContent = new ByteArrayInputStream(new byte[0]);
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName,
                    folder,
                    emptyContent,
                    metadata);
            s3client.putObject(putObjectRequest);
        }

        JsonObject ret = new JsonObject();
        ret.addProperty("status", "success");
        return ret;
    }

    @Override
    public JsonObject upload(String bucketName, String path, File file, String fileName) {
        JsonObject jo = new JsonObject();

        if (!s3client.doesBucketExist(bucketName)) {
            s3client.createBucket(bucketName);
        }

        if (file != null) {
            s3client.putObject(new PutObjectRequest(bucketName, path + fileName, file)
                    .withCannedAcl(CannedAccessControlList.Private));
        }

        jo.addProperty("status", "success");
        return jo;
    }
}
