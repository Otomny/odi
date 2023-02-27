package fr.omny.odi.proxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

import org.objectweb.asm.Opcodes;

import fr.omny.odi.Utils;
import fr.omny.odi.listener.OnProxyCallListener;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.loading.PackageDefinitionStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.matcher.ElementMatchers;

public class ProxyFactory {

	/**
	 * @param <T>
	 * @param clazz
	 * @param handler
	 * @return
	 * @throws Exception
	 */
	public static <T> T newProxyInstance(Class<? extends T> clazz, List<OnProxyCallListener> listeners) throws Exception {
		T instance = Utils.callConstructor(clazz);
		return newProxyInstance(clazz, instance, listeners);
	}

	/**
	 * @param <T>
	 * @param clazz
	 * @param handler
	 * @return
	 * @throws Exception
	 */
	public static <T> T newProxyInstance(Class<? extends T> clazz, T instance, List<OnProxyCallListener> listeners)
			throws Exception {

		InvocationHandler handler = new InvocationHandler() {

			@Override
			public Object invoke(Object proxyObj, Method proxyMethod, Object[] arguments) throws Throwable {
				String methodName = proxyMethod.getName();
				Class<?>[] parametersType = new Class<?>[proxyMethod.getParameters().length];
				for (int i = 0; i < parametersType.length; i++) {
					parametersType[i] = proxyMethod.getParameters()[i].getType();
				}
				Method remoteMethod = Utils.recursiveFindMethod(clazz, methodName, parametersType);
				var result = listeners.stream().filter(listener -> listener.pass(remoteMethod))
						.map(proxyListener -> {
							try {
								return proxyListener.invoke(instance, remoteMethod, arguments);
							} catch (Exception e) {
								throw new RuntimeException(e);
							}
						})
						.filter(o -> o != null)
						.findFirst()
						.orElse(null);
				if (proxyMethod.getReturnType() == void.class || result == null) {
					return remoteMethod.invoke(instance, arguments);
				} else {
					return result;
				}
			}

		};

		return newProxyInstance(clazz, instance, handler);
	}

	/**
	 * @param <T>
	 * @param clazz
	 * @param handler
	 * @return
	 * @throws Exception
	 */
	public static <T> T newProxyInstance(Class<? extends T> clazz, T originalInstance, InvocationHandler handler)
			throws Exception {
		Class<? extends T> proxyClass = new ByteBuddy().subclass(clazz)
				.implement(List.of(ProxyMarker.class))
				.defineField("originalClass", clazz.getClass(), Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL)
				.defineField("originalInstance", clazz, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL)
				.defineConstructor(Opcodes.ACC_PUBLIC)
				.withParameters(List.of(Class.class, clazz))
				.intercept(MethodCall.invoke(clazz.getDeclaredConstructor()).onSuper()
						.andThen(FieldAccessor.ofField("originalClass").setsArgumentAt(0))
						.andThen(FieldAccessor.ofField("originalInstance").setsArgumentAt(1)))
				.defineMethod("getOriginalClass", Class.class, Visibility.PUBLIC)
				.intercept(FieldAccessor.ofField("originalClass"))
				.defineMethod("getOriginalInstance", Object.class, Visibility.PUBLIC)
				.intercept(FieldAccessor.ofField("originalInstance"))
				.method(ElementMatchers.not(ElementMatchers.namedOneOf("getOriginalClass", "getOriginalInstance"))
						.and(ElementMatchers.any()))
				.intercept(InvocationHandlerAdapter.of(handler))
				.make().load(clazz.getClassLoader(),
						ClassLoadingStrategy.Default.INJECTION.with(PackageDefinitionStrategy.Trivial.INSTANCE))
				.getLoaded();

		return Utils.callConstructor(proxyClass.getConstructor(Class.class, clazz), proxyClass,
				new Object[] { clazz, originalInstance });
	}

	public static Class<?> getOriginalClass(Object proxyInstance) {
		if (proxyInstance instanceof ProxyMarker marker) {
			return marker.getOriginalClass();
		}
		return proxyInstance.getClass();
	}

	@SuppressWarnings("unchecked")
	public static <T> T getOriginalInstance(Object proxyInstance) {
		if (proxyInstance instanceof ProxyMarker marker) {
			return (T) marker.getOriginalInstance();
		}
		return (T) proxyInstance;
	}

}
