package main.java.multithreading;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import main.java.daemon.Monitor;
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
        if(engine==null){
            synchronized(SimEngine.class){
                if(engine==null){
                    engine = new SimEngine();
                    executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
                    executor.execute(engine);
                }else {
                    engine.sendNotification();
                }
            }
        }else {
            engine.sendNotification();
        }

        if(monitor==null){
            synchronized(SimEngine.class){
                if(monitor==null){
                    monitor = new Monitor();
                    singleExecutor = Executors.newSingleThreadExecutor();
                    singleExecutor.execute(monitor);
                }
            }
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
        JsonParser jp = new JsonParser();

        while(true){
            if(executor.isShutdown()){
                break;
            }

            /**
             * If there are too many simulations running, ignore this request/wake up
             */
            int runningSimulations = SimulationManager.INSTANCE.getRunningSimulation();
            if(runningSimulations>6){
                try {
                    this.wait();
                } catch (InterruptedException e) {}
                continue;
            }

            String jsonInString = null;
            try(RedisAccess access = RedisAccessFactory.getAccess()){
                //jsonInString = access.rpop("Brummitt_TaskQueue");
                jsonInString = access.rpop("TaskQueue");
            }catch (IOException e){
                LOG.error(e.getMessage(), e);
            }

            if (jsonInString != null) {
                JsonObject jo = jp.parse(jsonInString).getAsJsonObject();

                // run simulation
                executor.execute(new TaskRunner(jo));
                SimulationManager.counter.incrementAndGet();
            }else {
                try {
                    this.wait();
                } catch (InterruptedException e) {}
                continue;
            }
        }
        
        SimEngine.nullifyEngine();
    }
}
