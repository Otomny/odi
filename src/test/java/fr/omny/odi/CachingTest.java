package fr.omny.odi;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fr.omny.odi.caching.Caching;

public class CachingTest {

  @Before
  public void before() {
    Injector.startTest();
  }

  @After
  public void after() {
    Injector.wipeTest();
  }

  @Test
  public void test_Proxy_Caching() throws Exception {
    Injector.addSpecial(Service.class);
    var service = Injector.getService(Service.class);
    long start = System.currentTimeMillis();
    service.data("key");
    long end = System.currentTimeMillis();
    assertEquals(500L, end - start, 50L);
    start = System.currentTimeMillis();
    service.data("key");
    end = System.currentTimeMillis();
    assertEquals(0, end - start, 50L);
    start = System.currentTimeMillis();
    service.data("key2");
    end = System.currentTimeMillis();
    assertEquals(500L, end - start, 50L);
  }

  @Component
  public static class Service {

    @Caching()
    public String data(String s) {
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      return "Hello world";
    }
  }
}
