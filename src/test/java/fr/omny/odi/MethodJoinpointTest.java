package fr.omny.odi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.omny.odi.joinpoint.Joinpoint;
import fr.omny.odi.joinpoint.Pointcut;

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
	public void test_Joinpoint_Registered() throws Exception {
		Injector.addSpecial(Service.class);
		Injector.addSpecial(JoinpointListener.class);

		Service instance = Injector.getService(Service.class);
		JoinpointListener listener = Injector.getOriginalService(JoinpointListener.class);

		instance.callJoinPoint();

		assertNotNull(listener);
		assertTrue(listener.joinPointCalled);
		assertEquals(instance, listener.serviceInstance);
	}

	@Test
	public void test_Joinpoint_NonRegistered() throws Exception {
		Injector.addSpecial(JoinpointListener.class);

		Service instance = new Service();
		JoinpointListener listener = Injector.getOriginalService(JoinpointListener.class);

		instance.callJoinPoint();

		assertNotNull(listener);
		assertTrue(listener.joinPointCalled);
		assertEquals(instance, listener.serviceInstance);
	}

	@Test
	public void test_Joinpoint_Registered_Implicit() throws Exception {
		Injector.addSpecial(ServiceImplicit.class);
		Injector.addSpecial(JoinpointListener.class);

		ServiceImplicit instance = Injector.getService(ServiceImplicit.class);
		JoinpointListener listener = Injector.getOriginalService(JoinpointListener.class);

		instance.callJoinPoint();

		assertNotNull(listener);
		assertTrue(listener.joinPointCalled);
		assertEquals(instance, listener.serviceImplicitInstance);
	}

	@Component
	public static class Service {

		public void callJoinPoint() {
			Injector.joinpoint(this, "join");
		}

	}

	@Component
	public static class ServiceImplicit {

		@Pointcut()
		public void callJoinPoint() {
		}

	}

	@Component
	public static class JoinpointListener {

		boolean joinPointCalled;
		Service serviceInstance;
		ServiceImplicit serviceImplicitInstance;

		@Joinpoint(value = "join", on = Service.class)
		public void test(Service instance) {
			this.joinPointCalled = true;
			this.serviceInstance = instance;
		}

		@Joinpoint(value = "callJoinPoint", on = ServiceImplicit.class)
		public void testImplicit(ServiceImplicit instance) {
			this.joinPointCalled = true;
			this.serviceImplicitInstance = instance;
		}

	}

}
