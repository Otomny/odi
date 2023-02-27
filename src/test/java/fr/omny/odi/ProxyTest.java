package fr.omny.odi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import fr.omny.odi.caching.CacheProxyListener;
import fr.omny.odi.proxy.ProxyFactory;
import fr.omny.odi.proxy.ProxyMarker;

public class ProxyTest {

	@Test
	public void test_Get_OriginalClass() throws Exception {
		var originalService = Utils.callConstructor(Service.class);
		var service = ProxyFactory.newProxyInstance(Service.class, originalService, List.of(new CacheProxyListener()));
		assertEquals(Service.class, ProxyFactory.getOriginalClass(service));
	}

	@Test
	public void test_Proxy_Is() throws Exception {
		var originalService = Utils.callConstructor(Service.class);
		var service = ProxyFactory.newProxyInstance(Service.class, originalService, List.of(new CacheProxyListener()));
		assertFalse(originalService instanceof ProxyMarker);
		assertTrue(service instanceof ProxyMarker);
		assertTrue(Utils.isProxy(service));
	}

	public static class Service {

		public String data(String s) {
			return "Hello world";
		}

	}

}
