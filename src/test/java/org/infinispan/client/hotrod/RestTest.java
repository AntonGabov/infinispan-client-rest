package org.infinispan.client.hotrod;

import org.infinispan.client.rest.RestCacheManager;

public class RestTest {

   public static void main(String[] args) {
      RestCacheManager mng = new RestCacheManager("192.168.56.101", "8080");
      mng.getCache().put("Test1", "Cool");
      mng.getCache().put("Test2", "Story");
      
      System.out.println(mng.getCache().get("Test1"));
      System.out.println(mng.getCache().get("Test2"));
      mng.stop();
   }

}
