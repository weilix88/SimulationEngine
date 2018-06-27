package main.java.httpClientConnect;

import main.java.cloud.InstanceInfo;
import main.java.config.EngineConfig;
import main.java.multithreading.SimEngine;
import main.java.util.StringUtil;

public class StatusReporter {
    public static void sendStatus(String commitId, String parallelAgent, String status, String result){
        String watchDogURL = EngineConfig.readProperty("WatchDogURL");

        if(StringUtil.isNullOrEmpty(watchDogURL)){
            return;
        }

        HttpClient sender = new HttpClient();
        sender.setup(watchDogURL + "SimStatusCollector");
        sender.addParameter("commit_id", commitId);
        sender.addParameter("agent", parallelAgent);
        sender.addParameter("timestamp", String.valueOf(System.currentTimeMillis()));
        sender.addParameter("url", SimEngine.EngineID);
        sender.addParameter("status", status);
        sender.addParameter("result", result);
        sender.send(true);
    }

    public static void sendLog(String commitId, String parallelAgent, String msg, String type){
        String watchDogURL = EngineConfig.readProperty("WatchDogURL");

        if(StringUtil.isNullOrEmpty(watchDogURL)){
            return;
        }

        HttpClient sender = new HttpClient();
        sender.setup(watchDogURL + "SimLogCollector");
        sender.addParameter("commit_id", commitId);
        sender.addParameter("agent", parallelAgent);
        sender.addParameter("timestamp", String.valueOf(System.currentTimeMillis()));
        sender.addParameter("url", SimEngine.EngineID);
        sender.addParameter("msg", msg);
        sender.addParameter("type", type);
        sender.send(true);
    }
    
    public static void sendDaemonStatus(String msg, String type){
        String watchDogURL = EngineConfig.readProperty("WatchDogURL");

        if(StringUtil.isNullOrEmpty(watchDogURL)){
            return;
        }

        HttpClient sender = new HttpClient();
        sender.setup(watchDogURL + "DaemonInfoCollector");
        sender.addParameter("timestamp", String.valueOf(System.currentTimeMillis()));
        sender.addParameter("url", SimEngine.EngineID);
        sender.addParameter("msg", msg);
        sender.addParameter("type", type);
        sender.send(true);
    }

    public static void sendEngineLog(String msg, String type){
        String watchDogURL = EngineConfig.readProperty("WatchDogURL");

        if(StringUtil.isNullOrEmpty(watchDogURL)){
            return;
        }

        HttpClient sender = new HttpClient();
        sender.setup(watchDogURL + "EngineInfoCollector");
        sender.addParameter("timestamp", String.valueOf(System.currentTimeMillis()));
        sender.addParameter("url", SimEngine.EngineID);
        sender.addParameter("msg", msg);
        sender.addParameter("type", type);
        sender.send(true);
    }
}
