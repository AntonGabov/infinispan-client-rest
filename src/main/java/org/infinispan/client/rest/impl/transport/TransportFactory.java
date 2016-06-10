package org.infinispan.client.rest.impl.transport;

import java.util.List;

import org.infinispan.client.rest.configuration.Server;
import org.infinispan.client.rest.impl.transport.netty.NettyTransport;

public class TransportFactory {
      
   private List<Server> serverList;
   
   public void start(List<Server> serverList) {
      this.serverList = serverList;
   }
   
   public Transport getNettyTransport() {
      return new NettyTransport(serverList);
   }
}
