package org.infinispan.client.rest.impl.transport;

import java.util.concurrent.atomic.AtomicInteger;

import org.infinispan.client.rest.configuration.Configuration;

public interface Transport {
   
   void start(Configuration configuration, AtomicInteger initialTopologyId);
   
   void stop();
   
   void write(Object cacheName, Object key, Object value);
   
   Object read(Object cacheName, Object key);

}
