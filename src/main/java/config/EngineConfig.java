package main.java.config;

import java.util.ArrayList;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EngineConfig {
private static final Logger LOG = LoggerFactory.getLogger(EngineConfig.class);
    
    private static String ConfigPath = "";
    
    private static CompositeConfiguration properties = null;

    public static void setConfigPath(String path){
        EngineConfig.ConfigPath = path;
        readConfig();
    }
    
    private static void readConfig(){
        properties = new CompositeConfiguration();
        properties.addConfiguration(new SystemConfiguration());
        try {
            properties.addConfiguration(new PropertiesConfiguration(ConfigPath));
        } catch (ConfigurationException e) {
            LOG.error("Read configuration file failed, file path: "+ConfigPath);
            e.printStackTrace();
        }
    }
    
    public static ArrayList<String> readProperties(String[] keys){
        ArrayList<String> result = new ArrayList<String>();
        if(properties == null){
            readConfig();
        }
        for(String key : keys){
            result.add(properties.getString(key));
        }
        return result;
    }
    
    public static String readProperty(String key){
        if(properties == null){
            readConfig();
        }

        return properties.getString(key);
    }
}
