package org.infinispan.client.rest.impl.transport;

import org.infinispan.client.rest.configuration.Configuration;

public interface Transport {

   public static final String URI_BASIS = "/rest";
   
   void start(Configuration configuration);
   
   void stop();
   
   void write(Object cacheName, Object key, Object value);
   
   Object read(Object cacheName, Object key);
   
   default String getUri(Object... values) {
      StringBuilder bld = new StringBuilder(URI_BASIS);
      for (Object value : values) {
         if (bld.length() > 0) {
            bld.append("/");
         }
         bld.append(value);
      }
      return bld.toString();
   }

}
