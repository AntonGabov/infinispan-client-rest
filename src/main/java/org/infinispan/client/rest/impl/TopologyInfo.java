package org.infinispan.client.rest.impl;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import org.infinispan.client.rest.configuration.ServerConfiguration;
import org.infinispan.commons.logging.Log;
import org.infinispan.commons.logging.LogFactory;

public class TopologyInfo {
   
   private static final Log log = LogFactory.getLog(TopologyInfo.class);
   
   private AtomicInteger topologyId;
   private Collection<ServerConfiguration> servers;
   
   public TopologyInfo(AtomicInteger topologyId, Collection<ServerConfiguration> servers) {
      this.topologyId = topologyId;
      this.servers = servers;
   }
   
   public void updateTopologyId(int newTopolyId) {
      topologyId.set(newTopolyId);
   }
   
   public int getTopolyId() {
      return topologyId.get();
   }
   
   public Collection<ServerConfiguration> servers() {
      return servers;
   }
}
