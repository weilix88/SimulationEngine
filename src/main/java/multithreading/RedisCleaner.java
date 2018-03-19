package main.java.multithreading;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class RedisCleaner implements ServletContextListener{
    //private final Logger LOG = LoggerFactory.getLogger(RedisCleaner.class);
    
    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        /*try(Jedis jedis = new Jedis("localhost")){
            jedis.flushAll();
        }
        LOG.info("On servlet context destroyed, flush Redis.");
        
        SimEngine.shutdown();
        
        while(!SimEngine.isShutdown()){
            SimEngine.wakeSimEngine();
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
        }
        LOG.info("On servlet context destroyed, shut down simulation engine");*/
    }

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        
    }
}
