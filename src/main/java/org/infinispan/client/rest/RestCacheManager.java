package org.infinispan.client.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.infinispan.client.rest.api.RestCache;
import org.infinispan.client.rest.api.RestCacheContainer;
import org.infinispan.client.rest.configuration.Server;
import org.infinispan.client.rest.impl.RestCacheImpl;
import org.infinispan.client.rest.impl.transport.Transport;
import org.infinispan.client.rest.impl.transport.TransportFactory;
import org.infinispan.commons.logging.Log;
import org.infinispan.commons.logging.LogFactory;

public class RestCacheManager implements RestCacheContainer {

   private static final Log log = LogFactory.getLog(RestCacheManager.class);
   public static final String DEFAULT_CACHE_NAME = "default";

   private List<Server> serverList;
   protected Transport transport;

   private volatile boolean isStarted = false;
   private final Map<String, RestCache<?, ?>> cacheContainer = new HashMap<>();

   public RestCacheManager(List<Server> serverList) {
      this.serverList = serverList;
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
      TransportFactory transportFactory = new TransportFactory();
      transportFactory.start(serverList);
      transport = transportFactory.getNettyTransport();
      transport.start();
      log.info("RestManager is started");
      isStarted = true;
   }

   @Override
   public void stop() {
      synchronized (cacheContainer) {
         cacheContainer.clear();
      }
      transport.stop();
      
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
      RestCache<K, V> newCache = new RestCacheImpl<>(this, transport, cacheName);
      newCache.start();
      return newCache;
   }

}
