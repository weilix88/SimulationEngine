package main.java.cloud;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisCommands;
import redis.clients.jedis.JedisPool;

import java.io.Closeable;
import java.io.IOException;

public class RedisAccess implements Closeable{
	private JedisCommands commands;
	private JedisPool pool;
	private Closeable closeable;
	private static int EXPIRE = 24*60*60;

	private boolean isCluster = false;
	
	public RedisAccess(JedisCluster cluster){
		this.commands = cluster;
		this.closeable = cluster;

		isCluster = true;
	}

	public RedisAccess(Jedis jedis){
	    this.commands = jedis;
	    this.closeable = jedis;
    }
	
	public RedisAccess(JedisPool pool){
        this.pool = pool;
    }

	@Override
	public void close() {
	    if(isCluster && closeable!=null){
	        try {
                closeable.close();
            }catch(IOException ignore){}
        }
	}

	private class JedisWrapper implements Closeable{
	    JedisCommands wrapperCommands;

	    JedisWrapper() {
	        if(pool!=null && !pool.isClosed()){
                Jedis client = pool.getResource();
                wrapperCommands = client;
	            closeable = client;
            }else {
                wrapperCommands = commands;
            }
        }

        @Override
        public void close() {
            if(!isCluster && closeable!=null){
                try {
                    closeable.close();
                }catch(IOException ignore){}
            }
        }
    }

	public String set(String key, String value){
		try(JedisWrapper wrapper = new JedisWrapper()){
            return wrapper.wrapperCommands.setex(key, EXPIRE, value);
        }
	}
	
	public String get(String key){
        try(JedisWrapper wrapper = new JedisWrapper()) {
            return wrapper.wrapperCommands.get(key);
        }
	}
	
	public Long del(String key){
        try(JedisWrapper wrapper = new JedisWrapper()) {
            return wrapper.wrapperCommands.del(key);
        }
	}

	public Long rpush(String key, String value){
        try(JedisWrapper wrapper = new JedisWrapper()) {
            return wrapper.wrapperCommands.rpush(key, value);
        }
	}

	public String rpop(String key){
        try(JedisWrapper wrapper = new JedisWrapper()) {
            return wrapper.wrapperCommands.rpop(key);
        }
	}
	
	public Long expire(String key) {
        try(JedisWrapper wrapper = new JedisWrapper()) {
            return wrapper.wrapperCommands.expire(key, EXPIRE);
        }
	}
}
