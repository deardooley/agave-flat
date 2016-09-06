package org.iplantc.service.common.util;

import org.iplantc.service.common.Settings;
import org.iplantc.service.common.exceptions.AgaveCacheException;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class AgaveRedissonClient {

	private static RedissonClient redissonClient = null;
	
	/**
	 * Returns a pre-configured instance of a {@link RedissonClient}. 
	 * @return an active {@link RedissonClient}
	 * @throws AgaveCacheException if the {@link RedissonClient} cannot connect to the backing {@link RedissonNode}
	 */
	public static RedissonClient getInstance() 
	throws AgaveCacheException {
		if (redissonClient == null) {
			try {
				Config config = new Config();
				config.useSingleServer().setAddress(Settings.CACHE_HOST + ":" + Settings.CACHE_PORT);
				config.useSingleServer().setPassword(Settings.CACHE_PASSWORD);
				redissonClient = Redisson.create(config);
			}
			catch (Throwable e) {
				throw new AgaveCacheException("Failed to connect to redisson node.", e);
			}
		}
		
		return redissonClient;
	}
}
