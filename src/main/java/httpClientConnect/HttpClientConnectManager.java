package main.java.httpClientConnect;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class HttpClientConnectManager  implements ServletContextListener {
    private static final Logger LOG = LoggerFactory.getLogger(HttpClientConnectManager.class);

    private final static Object INST_CREATE_LOCK = new Object();
    private static PoolingHttpClientConnectionManager mgr = null;

    public static PoolingHttpClientConnectionManager getConnectionManager(){
        if(mgr == null){
            synchronized(INST_CREATE_LOCK){
                if(mgr == null){
                    mgr = new PoolingHttpClientConnectionManager();
                    mgr.setMaxTotal(10);                // Set the maximum number of total open connections.
                    mgr.setDefaultMaxPerRoute(4);      // Set the maximum number of concurrent connections per route
                    LOG.info("Connection manager finished initialization on request.");
                }
            }
        }

        return mgr;
    }

    public static void shutDown(){
        if(mgr!=null){
            mgr.shutdown();
        }
        LOG.info("Connection manager is shut down");
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        shutDown();
    }

    @Override
    public void contextInitialized(ServletContextEvent arg0) {}
}
