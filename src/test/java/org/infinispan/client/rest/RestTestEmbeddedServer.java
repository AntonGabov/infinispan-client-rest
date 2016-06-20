package org.infinispan.client.rest;

import java.util.LinkedList;
import java.util.List;

import org.infinispan.client.rest.configuration.ServerConfiguration;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.rest.RestServerTestBase;
import org.infinispan.rest.configuration.RestServerConfigurationBuilder;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RestTestEmbeddedServer extends RestServerTestBase {
   private static List<ServerConfiguration> serverList;
   private static RestCacheManager mng;

   @Before
   public void setup() throws Exception {
      EmbeddedCacheManager cm1 = TestCacheManagerFactory.createCacheManager();
      addServer("1", cm1, new RestServerConfigurationBuilder().port(8080).build());

      startServers();
      startCacheManager();
   }
   
   private void startCacheManager() {
      serverList = new LinkedList<ServerConfiguration>();
      serverList.add(new ServerConfiguration("127.0.0.1", "8080"));
      mng = new RestCacheManager();
   }

   @After 
   public void finish() throws Exception {
      stopServers();
      mng.stop();
   }

   @Test
   public void test() {
      String key1 = "Test1";
      String key2 = "Test2";
      String value1 = "Cool";
      String value2 = "Story";
      
      mng.getCache().put(key1, value1);
      mng.getCache().put(key2, value2);

      Assert.assertTrue(value1.equals((String) mng.getCache().get(key1)));
      Assert.assertTrue(value2.equals((String) mng.getCache().get(key2)));
   }
}
