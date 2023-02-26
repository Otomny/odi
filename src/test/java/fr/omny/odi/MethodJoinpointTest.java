package fr.omny.odi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MethodJoinpointTest {

	@BeforeEach
	public void setup() {
		Injector.startTest();
	}

	@AfterEach
	public void tearDown() {
		Injector.wipeTest();
	}

	@Test
	public void test_Joinpoint() throws Exception {
		Injector.addSpecial(Service.class);
		Injector.addSpecial(JoinpointListener.class);

		Service instance = Injector.getService(Service.class);
		JoinpointListener listener = Injector.getService(JoinpointListener.class);

		instance.callJoinPoint();

		assertNotNull(listener);
		assertTrue(listener.joinPointCalled);
		assertEquals(instance, listener.serviceInstance);
	}

	@Component
	public static class Service {

		public void callJoinPoint() {
			Injector.joinpoint(this, "join");
		}

	}

	@Component
	public static class JoinpointListener {

		boolean joinPointCalled;
		Service serviceInstance;

		@Joinpoint(value = "join", on = Service.class)
		public void test(Service instance) {
			this.joinPointCalled = true;
			this.serviceInstance = instance;
		}

	}

}
