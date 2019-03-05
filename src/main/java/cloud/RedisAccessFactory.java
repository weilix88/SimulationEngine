package main.java.cloud;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.java.config.EngineConfig;
import redis.clients.jedis.*;

public class RedisAccessFactory {
	private static final Logger LOG = LoggerFactory.getLogger(RedisAccessFactory.class);

	private static volatile JedisPool jedisPool;

	public static RedisAccess getAccess(){
		String detect = EngineConfig.readProperty("platform");
		if(detect==null){
			detect = "local";
		}

        Jedis client;
        switch(detect.toLowerCase()){
			case "aws":
				LOG.info("Connecting to AWS ElastiCache - get JedisCluster");
				Set<HostAndPort> hosts = new HashSet<>();
				int hostNum = Integer.parseInt(EngineConfig.readProperty("RedisHostNumber"));
				for(int i=0;i<hostNum;i++){
					hosts.add(new HostAndPort(EngineConfig.readProperty("RedisHost"+i),
							Integer.parseInt(EngineConfig.readProperty("RedisPort"+i))));
				}
				JedisCluster cluster = new JedisCluster(hosts);
				return new RedisAccess(cluster);
			case "azure":
				LOG.info("Connecting to Azure Redis cluster - get JedisPool");

				if(jedisPool==null || jedisPool.isClosed()){
				    synchronized (RedisAccessFactory.class){
				        if(jedisPool==null || jedisPool.isClosed()){
                            JedisPoolConfig config = new JedisPoolConfig();
                            config.setMaxTotal(20);
                            config.setMaxIdle(10);
				            jedisPool = new JedisPool(config,
                                    EngineConfig.readProperty("RedisAzureHost"),
                                    Integer.parseInt(EngineConfig.readProperty("RedisAzurePort")),
                                    20000,
                                    EngineConfig.readProperty("RedisAzurePassword"),
                                    true);
                        }
                    }
                }

                return new RedisAccess(jedisPool);
			default:
				LOG.info("Connecting to local Redis server - get Jedis");
				client = new Jedis("localhost");
				return new RedisAccess(client);
		}
	}

	public static void closePool(){
	    if(jedisPool!=null){
            jedisPool.close();
        }
    }
}
