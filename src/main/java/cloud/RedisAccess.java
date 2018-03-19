package main.java.cloud;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisCommands;

import java.io.Closeable;
import java.io.IOException;

public class RedisAccess implements Closeable{
	private JedisCommands commands;
	private Closeable closeable;
	private static int EXPIRE = 24*60*60;
	
	public RedisAccess(JedisCluster cluster){
		this.commands = cluster;
		this.closeable = cluster;
	}
	
	public RedisAccess(Jedis client){
		this.commands = client;
		this.closeable = client;
	}

	@Override
	public void close() throws IOException {
		closeable.close();
	}

	public String set(String key, String value){
		return commands.setex(key, EXPIRE, value);
	}
	
	public String get(String key){
		return commands.get(key);
	}
	
	public Long del(String key){
		return commands.del(key);
	}

	public Long rpush(String key, String value){
		return commands.rpush(key, value);
	}

	public String rpop(String key){
		return commands.rpop(key);
	}
	
	public Long expire(String key) {
		return commands.expire(key, EXPIRE);
	}
}
