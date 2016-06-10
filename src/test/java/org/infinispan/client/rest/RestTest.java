package org.infinispan.client.rest;

import java.util.LinkedList;
import java.util.List;

import org.infinispan.client.rest.RestCacheManager;
import org.infinispan.client.rest.configuration.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.junit.Assert;

public class RestTest {

   private static List<Server> serverList;
   private static RestCacheManager mng;

   @Before
   public void setup() {
      serverList = new LinkedList<Server>();
      serverList.add(new Server("127.0.0.1", "8080"));
      mng = new RestCacheManager(serverList);
   }
   
   @After 
   public void finish() {
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
