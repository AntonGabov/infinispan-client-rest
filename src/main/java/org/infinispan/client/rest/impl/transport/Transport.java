package org.infinispan.client.rest.impl.transport;

public interface Transport {

   void start();
   
   void stop();
   
   void write(Object cacheName, Object key, Object value);
   
   byte[] read(Object cacheName, Object key);

}
