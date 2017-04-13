package main.java.multithreading;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.google.gson.Gson;

import main.java.core.Task;
import redis.clients.jedis.Jedis;

public class SimEngine implements Runnable{
    //private final static Logger LOG = LoggerFactory.getLogger(SimEngine.class);
    
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
        Gson gson = new Gson();
        
        while(true){
            if(executor.isShutdown()){
                break;
            }
            
            String jsonInString = null;
            try(Jedis jedis = new Jedis("localhost")){
                jsonInString = jedis.rpop("TaskQueue");
                jedis.quit();
            }
            if (jsonInString != null) {
                Task task = gson.fromJson(jsonInString, Task.class);
                
                executor.execute(new TaskRunner(task));
            }else {
                try {
                    this.wait();
                } catch (InterruptedException e) {}
            }
        }
        
        SimEngine.nullifyEngine();
    }
}
