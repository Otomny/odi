package fr.omny.odi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.omny.odi.proxy.ProxyFactory;
import lombok.Getter;

public class FieldInjectionTest {

	@BeforeEach
	public void setupForEach() {
		Injector.startTest();
	}

	@AfterEach
	public void tearDownForEach() {
		Injector.wipeTest();
	}

	@Test
	public void test_FieldInjection()
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Injector.addService(DummyService.class, "default", Utils.callConstructor(DummyService.class));
		var client = new DummyClient1();
		assertNull(client.service);
		Injector.wire(client);
		assertNotNull(client.service);
	}

	@Test
	public void test_FieldInjection_Optional_NoService() {
		var client = new DummyClient2();
		assertNull(client.service);
		Injector.wire(client);
		assertNotNull(client.service);
		assertTrue(client.service.isEmpty());
	}

	@Test
	public void test_FieldInjection_Optional_Service()
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		var service = Utils.callConstructor(DummyService.class);
		Injector.addService(DummyService.class, "default", service);
		var client = new DummyClient2();
		assertNull(client.service);
		Injector.wire(client);
		assertNotNull(client.service);
		assertTrue(client.service.isPresent());
		assertEquals(service, ProxyFactory.getOriginalInstance(client.service.get()));
	}

	@Getter
	public static class DummyService {

	}

	@Getter
	public static class DummyClient1 {

		@Autowired
		private DummyService service;

	}

	@Getter
	public static class DummyClient2 {

		@Autowired
		private Optional<DummyService> service;
	}

}
