package main.java.aws.meta;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import main.java.config.EngineConfig;

public class SDKEndPoint {
    protected AmazonS3 s3client;
    public SDKEndPoint(Regions r) {
        AmazonS3ClientBuilder s3ClientBuilder = AmazonS3ClientBuilder.standard();
        
        String region;
        if(r==null){
            region = EngineConfig.readProperty("DefaultAWSRegion");
        }else {
            region = r.toString();
        }

        s3ClientBuilder.setRegion(region);
        
        AWSCredentialsProvider provider = DefaultAWSCredentialsProviderChain.getInstance();
        s3client = s3ClientBuilder.withCredentials(provider).build();
    }
}
