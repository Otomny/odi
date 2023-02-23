package fr.omny.odi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import fr.omny.odi.caching.CacheProxyListener;
import fr.omny.odi.caching.Caching;
import fr.omny.odi.proxy.ProxyFactory;
import fr.omny.odi.proxy.ProxyMarker;

public class ProxyTest {

	@Test
	public void test_Proxy_Is() throws Exception {
		var originalService = Utils.callConstructor(Service.class);
		var service = ProxyFactory.newProxyInstance(Service.class, originalService, List.of(new CacheProxyListener()));
		assertFalse(originalService instanceof ProxyMarker);
		assertTrue(service instanceof ProxyMarker);
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
