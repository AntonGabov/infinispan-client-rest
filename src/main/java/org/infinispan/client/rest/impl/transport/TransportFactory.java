package org.infinispan.client.rest.impl.transport;

import org.infinispan.client.rest.impl.transport.netty.NettyTransport;

public class TransportFactory {
      
   private String host;
   private String port;
   
   public void start(String host, String port) {
      this.host = host;
      this.port = port;
   }
   
   public Transport getTransport(String key) {
      Transport transport = null;
      switch (key) {
         default:
            transport = new NettyTransport(host, port);
      }
      return transport;
   }
}
