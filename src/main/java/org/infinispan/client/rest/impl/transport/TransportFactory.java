package org.infinispan.client.rest.impl.transport;

import java.util.List;

import org.infinispan.client.rest.configuration.ServerConfiguration;
import org.infinispan.client.rest.impl.transport.http.HttpTransport;
import org.infinispan.client.rest.impl.transport.netty.NettyTransport;

public class TransportFactory {
      
   private List<ServerConfiguration> serverList;
   
   public void start(List<ServerConfiguration> serverList) {
      this.serverList = serverList;
   }
   
   public Transport getNettyTransport() {
      return new NettyTransport();
   }
   
   public Transport getHttpTransport() {
      return new HttpTransport();
   }
}
