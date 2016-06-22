package org.infinispan.client.rest.impl.transport.http;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.infinispan.client.rest.configuration.Configuration;
import org.infinispan.client.rest.configuration.ServerConfiguration;
import org.infinispan.client.rest.impl.TopologyInfo;
import org.infinispan.client.rest.impl.protocol.CustomHttpHeaderNames;
import org.infinispan.client.rest.impl.transport.Transport;
import org.infinispan.client.rest.impl.transport.operations.HttpOperationsFactory;
import org.infinispan.client.rest.marshall.MarshallUtil;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

public class HttpTransport implements Transport {

   //private static final Log log = LogFactory.getLog(HttpTransport.class);

   private static final int MAX_CONTENT_LENGTH = 10 * 1024 * 1024;

   private Configuration configuration;
   private List<ServerConfiguration> initialServers = new LinkedList<ServerConfiguration>();
   private volatile TopologyInfo topologyInfo;
   private HttpOperationsFactory operations;

   @Override
   public void start(Configuration configuration, AtomicInteger initialTopologyId) {
      this.configuration = configuration;
      initialServers.addAll(configuration.servers());
      topologyInfo = new TopologyInfo(initialTopologyId, initialServers);
      setupOperations();
   }

   private void setupOperations() {
      operations = new HttpOperationsFactory();
      operations.start();
   }

   @Override
   public void stop() {
      operations.stop();
   }

   @Override
   public void write(Object cacheName, Object key, Object value) {
      topologyInfo.servers().forEach(server -> write(server, cacheName, key, value));
   }

   private void write(ServerConfiguration server, Object cacheName, Object key, Object value) {
      HttpResponse response = operations.putRequest(topologyInfo, server, cacheName, key, value);
      if (response != null && HttpResponseStatus.OK.equals(response.status())) {
         checkTopologyId(response);
      }
   }

   @Override
   public Object read(Object cacheName, Object key) {
      Object data = null;
      Iterator<ServerConfiguration> iterator = topologyInfo.servers().iterator();
      while (iterator.hasNext()) {
         byte[] readInfo = read(iterator.next(), cacheName, key);
         if (readInfo != null) {
            data = MarshallUtil.byteArray2Object(readInfo);
            break;
         }
      }
      return data;
   }

   private byte[] read(ServerConfiguration server, Object cacheName, Object key) {
      byte[] data = null;

      FullHttpResponse response = (FullHttpResponse) operations.getRequest(topologyInfo, server, cacheName, key, MAX_CONTENT_LENGTH);

      if (response != null && HttpResponseStatus.OK.equals(response.status())) {
         ByteBuf content = response.content();
         data = new byte[content.readableBytes()];
         content.readBytes(data);
         response.release();
         checkTopologyId(response);
      }

      return data;
   }
   
   private void checkTopologyId(HttpResponse response) {
      Integer retrievedTopologyId = response.headers().getInt(CustomHttpHeaderNames.TOPOLOGY_ID);
      if (retrievedTopologyId != null && retrievedTopologyId.equals(topologyInfo.getTopolyId())) {
         topologyInfo.updateTopologyId(retrievedTopologyId);
      }
   }

}
