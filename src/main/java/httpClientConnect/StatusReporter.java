package main.java.httpClientConnect;

import main.java.cloud.InstanceInfo;
import main.java.config.EngineConfig;

public class StatusReporter {
    public static void sendStatus(String commitId, String parallelAgent, String status, String result){
        String watchDogURL = EngineConfig.readProperty("WatchDogURL");

        HttpClient sender = new HttpClient();
        sender.setup(watchDogURL + "SimStatusCollector");
        sender.addParameter("commit_id", commitId);
        sender.addParameter("agent", parallelAgent);
        sender.addParameter("timestamp", String.valueOf(System.currentTimeMillis()));
        sender.addParameter("url", InstanceInfo.getPublicIP());
        sender.addParameter("status", status);
        sender.addParameter("result", result);
        sender.send(true);
    }

    public static void sendLog(String commitId, String parallelAgent, String msg, String type){
        String watchDogURL = EngineConfig.readProperty("WatchDogURL");

        HttpClient sender = new HttpClient();
        sender.setup(watchDogURL + "SimLogCollector");
        sender.addParameter("commit_id", commitId);
        sender.addParameter("agent", parallelAgent);
        sender.addParameter("timestamp", String.valueOf(System.currentTimeMillis()));
        sender.addParameter("url", InstanceInfo.getPublicIP());
        sender.addParameter("msg", msg);
        sender.addParameter("type", type);
        sender.send(true);
    }
}
