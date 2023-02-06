package fr.omny.odi;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import lombok.Getter;

public class FieldInjectionTest {

	@Before
	public void setupForEach() {
		Injector.startTest();
	}

	@After
	public void tearDownForEach() {
		Injector.wipeTest();
	}

	@Test
	public void test_FieldInjection()
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Injector.addService(DummyService.class, Utils.callConstructor(DummyService.class));
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
		Injector.addService(DummyService.class, service);
		var client = new DummyClient2();
		assertNull(client.service);
		Injector.wire(client);
		assertNotNull(client.service);
		assertTrue(client.service.isPresent());
		assertEquals(service, client.service.get());
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
