package fr.omny.odi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import lombok.Getter;

public class ConstructorInjectionTest {

	@Test
	public void callConstructor_NoArgument()
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		var service = Utils.callConstructor(DummyService1.class);
		assertNotNull(service);
		assertNull(service.getData());
	}

	@Test
	public void callConstructor_Argument()
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		var service = Utils.callConstructor(DummyService1.class, false, "Hello world");
		assertNotNull(service);
		assertEquals("Hello world", service.getData());
	}

	@Test
	public void callConstructor_Argument_Autowiring()
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Injector.startTest();
		Injector.addServiceParams(DummyService1.class, "default", "Hello");

		var service2 = Utils.callConstructor(DummyService2.class);
		assertEquals("Hello", service2.getData());

		Injector.wipeTest();
	}

	@Test
	public void callConstructor_Complex()
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		var service = Utils.callConstructor(ComplexService.class, false, UUID.class, String.class,
				new Function<String, Integer>() {
					@Override
					public Integer apply(String a) {
						return 50;
					}
				});
		assertNotNull(service);
		assertEquals(UUID.class, service.getIdClass());
		assertEquals(String.class, service.getDataClass());
		assertNotNull(service.getMapping());
		assertEquals(50, service.getMapping().apply("").intValue());
	}

	public static class DummyService1 {

		private String data;

		DummyService1(){}

		public DummyService1(String data) {
			this.data = data;
		}

		public String getData() {
			return data;
		}

	}

	@Getter
	public static class ComplexService {

		private Class<?> idClass;
		private Class<?> dataClass;
		private Function<String, Integer> mapping;

		public ComplexService(Class<?> idClass, Class<?> dataClass, Function<String, Integer> mapping) {
			this.idClass = idClass;
			this.dataClass = dataClass;
			this.mapping = mapping;
		}

	}

	public static class DummyService2 {

		private String data;

		DummyService2(){}

		public DummyService2(@Autowired DummyService1 serv) {
			this.data = serv.getData();
		}

		public String getData() {
			return data;
		}

	}

}
