package fr.omny.odi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import fr.omny.odi.proxy.ProxyFactory;
import fr.omny.odi.proxy.ProxyMarker;

public class ProxyTest {

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
	public void test_Proxy_Call() throws Exception {
		var service = ProxyFactory.newProxyInstance(Service.class);
		assertTrue(service instanceof ProxyMarker);
		assertEquals("Hello world", service.data());
	}

	@Test
	public void test_Proxy_Call_MultipleMethods() throws Exception {
		var service = ProxyFactory.newProxyInstance(Service2.class);
		assertTrue(service instanceof ProxyMarker);
		assertEquals("Hello world", service.data());
		assertEquals("Hello world space", service.data("space"));
		assertEquals("Hello world space/69", service.data("space", 69));
	}

	@Test
	public void test_Proxy_ConstructorCall() throws Exception {
		Injector.startTest();
		var originalService = Utils.callConstructor(Service3.class);
		Injector.addService(Service3.class, originalService);
		var service = Injector.getService(Service3.class);
		assertFalse(originalService instanceof ProxyMarker);
		assertFalse(service instanceof ProxyMarker);
		assertFalse(Utils.isProxy(service));
		assertEquals(1, Service3.CALL_COUNT.get());
		Injector.wipeTest();
	}

	public static class Service {

		public String data() {
			return "Hello world";
		}

	}

	public static class Service2 {

		public String data() {
			return "Hello world";
		}

		public String data(String s) {
			return "Hello world " + s;
		}

		public String data(String s, int i) {
			return "Hello world " + s + "/" + i;
		}

	}

	@Component(proxy = false)
	public static class Service3 {

		public static final AtomicInteger CALL_COUNT = new AtomicInteger(0);

		public Service3() {
			CALL_COUNT.incrementAndGet();
		}

	}

}
