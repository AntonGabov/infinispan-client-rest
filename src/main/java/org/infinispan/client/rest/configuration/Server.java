package org.infinispan.client.rest.configuration;

public class Server {

   private String host;
   private String port;
   
   public Server(String host, String port) {
      this.host = host;
      this.port = port;
   }
   
   public String getHost() {
      return host;
   }

   public String getPort() {
      return port;
   }
}
