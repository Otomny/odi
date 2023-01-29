package fr.omny.odi;


import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public class Injector {

	private static Injector instance;
	private static Optional<Logger> logger;

	public static Optional<Logger> getLogger(){
		return logger;
	}

	public static void startApplication(Class<?> mainClass) {
		startApplication(mainClass, null);
	}

	public static void startApplication(Class<?> mainClass, Logger logger) {
		try {
			synchronized (Injector.class) {
				if (instance == null) {
					instance = new Injector();
					Injector.logger = Optional.ofNullable(logger);
					instance.add(mainClass);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Perform autowiring
	 * 
	 * @param object
	 */
	public static void wire(Object object) {
		Utils.autowireNoException(object);
	}

	public static <T> T getService(Class<T> klass) {
		try {
			return instance.getServiceInstance(klass);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void addFrom(Class<?> mainClass) {
		if (instance != null) {
			instance.add(mainClass);
		}
	}

	/**
	 * All instances of a service
	 */
	private Map<Class<?>, Object> singletons;

	private Injector() {
		singletons = new HashMap<>();
	}

	public void add(Class<?> mainClass) {
		var classes = Utils.getClasses(mainClass.getPackageName(), klass -> klass.isAnnotationPresent(Component.class));
		for (Class<?> implementationClass : classes) {
			try {
				Object serviceInstance = implementationClass.getConstructor().newInstance();
				this.singletons.put(implementationClass, serviceInstance);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
					| NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Retrieve the service instance
	 * 
	 * @param <T>
	 * @param serviceClass
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <T> T getServiceInstance(Class<?> serviceClass) {
		if (this.singletons.containsKey(serviceClass)) {
			return (T) this.singletons.get(serviceClass);
		} else {
			return null;
		}
	}

}
