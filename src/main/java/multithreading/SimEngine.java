package main.java.multithreading;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.google.gson.Gson;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import main.java.aws.redis.RedisAccess;
import main.java.aws.redis.RedisAccessFactory;
import main.java.core.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

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
        //Gson gson = new Gson();
        JsonParser jp = new JsonParser();

        while(true){
            if(executor.isShutdown()){
                break;
            }
            
            String jsonInString = null;
            try(RedisAccess access = RedisAccessFactory.getAccess()){
                jsonInString = access.rpop("TaskQueue");
            }catch (IOException e){
                LOG.error(e.getMessage(), e);
            }

            if (jsonInString != null) {
                //Task task = gson.fromJson(jsonInString, Task.class);
                JsonObject jo = jp.parse(jsonInString).getAsJsonObject();
                executor.execute(new TaskRunner(jo));
            }else {
                try {
                    this.wait();
                } catch (InterruptedException e) {}
            }
        }
        
        SimEngine.nullifyEngine();
    }
}
