package main.java.multithreading;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import main.java.aws.redis.RedisAccess;
import main.java.aws.redis.RedisAccessFactory;

public class SimEngine implements Runnable{
    private final static Logger LOG = LoggerFactory.getLogger(SimEngine.class);
    
    private static SimEngine engine = null;
    private static ThreadPoolExecutor executor = null;
    
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
                jsonInString = access.rpop("TaskQueue");
                System.out.println("Read from task queue: "+jsonInString);
            }catch (IOException e){
                LOG.error(e.getMessage(), e);
            }

            if (jsonInString != null) {
                JsonObject jo = jp.parse(jsonInString).getAsJsonObject();

                // run simulation
                executor.execute(new TaskRunner(jo));
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
