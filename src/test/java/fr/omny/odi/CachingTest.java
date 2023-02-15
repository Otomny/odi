package fr.omny.odi;


import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import fr.omny.odi.caching.CacheProxyListener;
import fr.omny.odi.caching.Caching;
import fr.omny.odi.proxy.ProxyFactory;

public class CachingTest {

	@Test
	public void test_Cache_Key() throws Exception {
		var service = ProxyFactory.newProxyInstance(Service.class, List.of(new CacheProxyListener()));
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
