package fr.omny.odi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.omny.odi.caching.Caching;
import fr.omny.odi.proxy.ProxyFactory;

public class CachingTest {

	@BeforeEach
	public void before() {
		Injector.startTest();
	}

	@AfterEach
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

	@Test
	public void test_Proxy_NativeCall() throws Exception {

		Injector.addSpecial(Service.class);
		var service = Injector.getService(Service.class);
		var originalService = ProxyFactory.getOriginalInstance(service);
		assertEquals(originalService.hashCode(), service.hashCode());
		assertEquals(originalService.toString(), service.toString());
		assertTrue(service.equals(originalService));
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
