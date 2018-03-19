package main.java.cloud;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import main.java.config.EngineConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisShardInfo;

public class RedisAccessFactory {
	private static final Logger LOG = LoggerFactory.getLogger(RedisAccessFactory.class);
	public static RedisAccess getAccess(){
		String detect = EngineConfig.readProperty("platform");
		if(detect==null){
			detect = "local";
		}
		switch(detect.toLowerCase()){
			case "aws":
				LOG.info("Connecting to AWS ElastiCache");
				Set<HostAndPort> hosts = new HashSet<>();
				int hostNum = Integer.parseInt(EngineConfig.readProperty("RedisHostNumber"));
				for(int i=0;i<hostNum;i++){
					hosts.add(new HostAndPort(EngineConfig.readProperty("RedisHost"+i),
							Integer.parseInt(EngineConfig.readProperty("RedisPort"+i))));
				}
				JedisCluster cluster = new JedisCluster(hosts);
				return new RedisAccess(cluster);
			case "azure":
				LOG.info("Connecting to Azure Redis cluster");
				JedisShardInfo shardInfo = new JedisShardInfo(EngineConfig.readProperty("RedisAzureHost"), 
						Integer.parseInt(EngineConfig.readProperty("RedisAzurePort")), 
						true);
				shardInfo.setPassword(EngineConfig.readProperty("RedisAzurePassword"));
				Jedis azureClient = new Jedis(shardInfo);
				return new RedisAccess(azureClient);
			default:
				LOG.info("Connecting to local Redis server");
				Jedis client = new Jedis("localhost");
				return new RedisAccess(client);
		}
	}
}
