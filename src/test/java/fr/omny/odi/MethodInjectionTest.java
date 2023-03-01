package fr.omny.odi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;

public class MethodInjectionTest {

	@Test
	public void callMethod_NoArgumentPassed()
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		boolean result = (boolean) Utils.callMethod("testMethod1", this);
		assertTrue(!result);
	}

	@Test
	public void callMethod_ArgumentPassed()
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		boolean result = (boolean) Utils.callMethod("testMethod1", this, new Dummy1(""));
		assertTrue(result);
	}

	public boolean testMethod1(Dummy1 dum) {
		return dum != null;
	}

	@Test
	public void callMethod_ArgumentPassed_Autowiring()
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Injector.startTest();
		Injector.addService(DummyService.class, new DummyService("Hello world"));

		String result = (String) Utils.callMethod("testMethod2", this, new Dummy1(""));

		assertEquals("Hello world", result);

		Injector.wipeTest();
	}

	public String testMethod2(Dummy1 dum, @Autowired DummyService serv) {
		if (dum == null || serv == null)
			return null;
		return serv.getData();
	}

	public static class DummyService {

		private String data;


		/**
		 * 
		 */
		public DummyService() {
		}

		public DummyService(String data) {
			this.data = data;
		}

		public String getData() {
			return data;
		}

	}

	public record Dummy1(String data) {
	}

}