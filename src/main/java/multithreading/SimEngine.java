package main.java.multithreading;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import main.java.daemon.Monitor;
import main.java.httpClientConnect.StatusReporter;
import main.java.util.ProcessUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import main.java.cloud.RedisAccess;
import main.java.cloud.RedisAccessFactory;

public class SimEngine implements Runnable{
    private final static Logger LOG = LoggerFactory.getLogger(SimEngine.class);
    
    private static volatile SimEngine engine = null;
    private static ThreadPoolExecutor executor = null;

    private static volatile Monitor monitor = null;
    private static ExecutorService singleExecutor = null;
    
    public static void wakeSimEngine(){
        try {
            if (engine == null) {
                synchronized (SimEngine.class) {
                    if (engine == null) {
                        engine = new SimEngine();
                        executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
                        executor.execute(engine);
                    } else {
                        engine.sendNotification();
                    }
                }
            } else {
                engine.sendNotification();
            }

            if (monitor == null || !monitor.running) {
                synchronized (SimEngine.class) {
                    if (monitor == null || !monitor.running) {
                        monitor = new Monitor();
                        singleExecutor = Executors.newSingleThreadExecutor();
                        singleExecutor.execute(monitor);
                    }
                }
            }
        }catch (Throwable e){
            StatusReporter.sendEngineLog("Engine wake encounters error: "+e.getMessage(), "error");
            engine.sendNotification();
        }
    }
    
    protected synchronized void sendNotification(){
        this.notify();
    }
    
    public static void shutdown(){
        if(executor!=null && !executor.isShutdown()){
            executor.shutdownNow();
        }
    }
    
    public static boolean isShutdown(){
        return engine==null;
    }
    
    private static void nullifyEngine(){
        engine = null;
    }

    @Override
    public synchronized void run() {
        try {
            JsonParser jp = new JsonParser();

            while (true) {
                /*if(executor.isShutdown()){
                    break;
                }*/

                /**
                 * If there are too many simulations running, ignore this request/wake up
                 */
                int runningSimulations = SimulationManager.INSTANCE.getRunningSimulation();
                if (runningSimulations > 6) {
                    List<String> runningEplus = ProcessUtil.getPIDs();
                    int runningEplusNum = runningEplus.size();

                    StatusReporter.sendEngineLog("Engine running full number simulation: "+runningSimulations+" vs "+runningEplusNum, "info");

                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                    }
                    continue;
                }

                String jsonInString = null;
                try (RedisAccess access = RedisAccessFactory.getAccess()) {
                    jsonInString = access.rpop("TaskQueue");
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }

                if (jsonInString != null) {
                    JsonObject jo;

                    try {
                        jo = jp.parse(jsonInString).getAsJsonObject();
                    }catch (JsonParseException e){
                        StatusReporter.sendEngineLog("Input json str not valid: "+jsonInString+", "+e.getMessage(), "error");
                        continue;
                    }

                    // run simulation
                    executor.execute(new TaskRunner(jo));
                    SimulationManager.counter.incrementAndGet();
                } else {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                    }
                    continue;
                }
            }
        }catch (Throwable e){
            StatusReporter.sendEngineLog("Engine encounters error and break while-true: "+e.getMessage(), "error");
            SimEngine.nullifyEngine();
        }
    }
}
