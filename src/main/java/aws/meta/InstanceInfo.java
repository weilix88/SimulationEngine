package main.java.aws.meta;

import main.java.config.EngineConfig;
import main.java.util.NetworkRequester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class InstanceInfo {
    private static volatile String publicIP = "";
    private static volatile String instanceID = "";

    public static String getPublicIP(){
        String platform = EngineConfig.readProperty("platform");
        if(platform.equalsIgnoreCase("aws")) {
            if (publicIP.isEmpty()) {
                synchronized (InstanceInfo.class) {
                    if (publicIP.isEmpty()) {
                        publicIP = queryPublicIP();
                    }
                }
            }
        }else {
            publicIP = "127.0.0.1:8080";
        }

        return publicIP;
    }

    public static String getInstanceID(){
        String platform = EngineConfig.readProperty("platform");
        if(platform.equalsIgnoreCase("aws")) {
            if (instanceID.isEmpty()) {
                synchronized (InstanceInfo.class) {
                    if (instanceID.isEmpty()) {
                        instanceID = queryInstanceId();
                    }
                }
            }
        }else {
            instanceID = "1";
        }

        return instanceID;
    }

    private static String queryPublicIP(){
        return NetworkRequester.get("http://"+ EngineConfig.readProperty("metaDataQuery")+"public-ipv4");
    }

    private static String queryInstanceId(){
        return NetworkRequester.get("http://"+ EngineConfig.readProperty("metaDataQuery")+"instance-id");
    }
}
