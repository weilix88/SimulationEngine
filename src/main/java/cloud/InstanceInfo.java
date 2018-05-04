package main.java.cloud;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import main.java.config.EngineConfig;
import main.java.util.NetworkRequester;

public class InstanceInfo {
    private static volatile String publicIP = "";
    private static volatile String instanceID = "";

    public static String getPublicIP(){
        String platform = EngineConfig.readProperty("platform");
        if(platform.equalsIgnoreCase("aws")
        		|| platform.equalsIgnoreCase("azure")) {
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
        if(platform.equalsIgnoreCase("aws") || platform.equalsIgnoreCase("azure")) {
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
    
    public static String getVMName(){
    	String platform = EngineConfig.readProperty("platform");
    	if(platform.equalsIgnoreCase("azure")) {
    		String[][] headers = {{"MetaData", "true"}};
    		String response = NetworkRequester.get("http://"+ EngineConfig.readProperty("InstanceMetaDataQueryAzure"), headers);
    		if(response!=null && !response.isEmpty()) {
    			response = response.substring(response.indexOf("{"));
    			
    			JsonParser jp = new JsonParser();
    	        JsonObject jo = jp.parse(response).getAsJsonObject();
    	        return jo.get("name").getAsString();
    		}
    	}
    	return "sim-vm";
    }

    private static String queryPublicIP(){
    	String platform = EngineConfig.readProperty("platform");
    	if(platform.equalsIgnoreCase("aws")) {
    		return NetworkRequester.get("http://"+ EngineConfig.readProperty("metaDataQuery")+"public-ipv4", null);
    	}else if(platform.equalsIgnoreCase("azure")) {
    		String[][] headers = {{"MetaData", "true"}};
    		String response = NetworkRequester.get("http://"+ EngineConfig.readProperty("NetworkMetaDataQueryAzure"), headers);
    		if(response!=null && !response.isEmpty()) {
    			JsonParser jp = new JsonParser();
    	        JsonObject jo = jp.parse(response).getAsJsonObject();
    	        return jo.get("publicIpAddress").getAsString();
    		}
    	}
    	return "127.0.0.1:8080";
    }

    private static String queryInstanceId(){
    	String platform = EngineConfig.readProperty("platform");
    	if(platform.equalsIgnoreCase("aws")) {
    		return NetworkRequester.get("http://"+ EngineConfig.readProperty("metaDataQuery")+"instance-id", null);
    	}else if(platform.equalsIgnoreCase("azure")) {
    		String[][] headers = {{"MetaData", "true"}};
    		String response = NetworkRequester.get("http://"+ EngineConfig.readProperty("InstanceMetaDataQueryAzure"), headers);
    		if(response!=null && !response.isEmpty()) {
    			response = response.substring(response.indexOf("{"));
    			
    			JsonParser jp = new JsonParser();
    	        JsonObject jo = jp.parse(response).getAsJsonObject();
    	        
    	        return jo.get("vmId").getAsString();
    		}
    	}
    	return "1";
    }
}
