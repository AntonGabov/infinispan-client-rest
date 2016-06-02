package org.infinispan.client.rest;

import java.util.HashMap;
import java.util.Map;

import org.infinispan.client.rest.api.RestCache;
import org.infinispan.client.rest.api.RestCacheContainer;
import org.infinispan.client.rest.impl.RestCacheImpl;
import org.infinispan.commons.logging.Log;
import org.infinispan.commons.logging.LogFactory;

// TODO: Implement operations and transport factories!
public class RestCacheManager implements RestCacheContainer {

   private static final Log log = LogFactory.getLog(RestCacheManager.class);
   public static final String DEFAULT_CACHE_NAME = "___defaultcache";
   public static final String DEFAULT_HOST = "127.0.0.1";
   public static final String DEFAULT_PORT = "8080";

   private String host;
   private String port;

   private volatile boolean isStarted = false;
   private final Map<String, RestCache<?, ?>> cacheContainer = new HashMap<>();

   private

   RestCacheManager(String host, String port) {
      this.host = host;
      this.port = port;
      start();
   }

   @Override
   public <K, V> RestCache<K, V> getCache() {
      return getCache(DEFAULT_CACHE_NAME);
   }

   @Override
   public <K, V> RestCache<K, V> getCache(String cacheName) {
      synchronized (cacheContainer) {
         RestCache<K, V> cache = null;
         if (!cacheContainer.containsKey(cacheName)) {
            cache = createCache(cacheName);
            cacheContainer.put(cacheName, cache);
         } else {
            cache = (RestCache<K, V>) cacheContainer.get(cacheName);
         }

         return cache;
      }
   }

   @Override
   public void start() {
      log.info("RestManager is started");
      isStarted = true;
   }

   @Override
   public void stop() {
      synchronized (cacheContainer) {
         cacheContainer.clear();
      }

      log.info("RestManager is stopped");
      isStarted = false;
   }

   public boolean isStarted() {
      return isStarted;
   }

   /**
    * 
    * Create a new instance of {@link org.infinispan.client.rest.impl.RestCacheImpl}
    * 
    * @param cacheName
    * @return a cache instance
    */
   private <K, V> RestCache<K, V> createCache(String cacheName) {
      RestCache<K, V> newCache = new RestCacheImpl<>(this, cacheName);
      newCache.start();
      return newCache;
   }

}
