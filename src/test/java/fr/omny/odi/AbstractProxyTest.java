package fr.omny.odi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import fr.omny.odi.proxy.ProxyFactory;
import fr.omny.odi.proxy.ProxyMarker;

public class AbstractProxyTest {

	@Test
	public void test_Get_OriginalClass() throws Exception {
		var originalService = Utils.callConstructor(Service.class);
		var service = ProxyFactory.newProxyInstance(Service.class, originalService);
		assertEquals(Service.class, ProxyFactory.getOriginalClass(service));
	}

	@Test
	public void test_Proxy_Is() throws Exception {
		var originalService = Utils.callConstructor(Service.class);
		var service = ProxyFactory.newProxyInstance(Service.class, originalService);
		assertFalse(originalService instanceof ProxyMarker);
		assertTrue(service instanceof ProxyMarker);
		assertTrue(Utils.isProxy(service));
	}

	@Test
	public void test_Proxy_HashCode() throws Exception {
		var originalService = Utils.callConstructor(Service.class);
		AService service = ProxyFactory.newProxyInstance(AService.class, originalService);
		assertFalse(originalService instanceof ProxyMarker);
		assertTrue(service instanceof ProxyMarker);
		assertTrue(Utils.isProxy(service));
		assertEquals(originalService.hashCode(), service.hashCode());
	}

	public static abstract class AService {

	}

	public static class Service extends AService {

		public String data() {
			return "Hello world";
		}

	}

}
