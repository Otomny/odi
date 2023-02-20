package fr.omny.odi.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

import fr.omny.odi.Utils;
import fr.omny.odi.listener.OnProxyCallListener;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.loading.PackageDefinitionStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
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
				if (proxyMethod.getReturnType() == void.class) {
					return result;
				}
				if (result != null) {
					return result;
				} else {
					return remoteMethod.invoke(instance, arguments);
				}
			}

		};

		Class<? extends T> proxyClass = new ByteBuddy().subclass(clazz)
				.method(ElementMatchers.any())
				.intercept(InvocationHandlerAdapter.of(handler)).make().load(clazz.getClassLoader(),
						ClassLoadingStrategy.Default.INJECTION.with(PackageDefinitionStrategy.Trivial.INSTANCE))
				.getLoaded();

		return proxyClass.getConstructor().newInstance();
	}

	/**
	 * @param <T>
	 * @param clazz
	 * @param handler
	 * @return
	 * @throws Exception
	 */
	public static <T> T newProxyInstance(Class<? extends T> clazz, InvocationHandler handler) throws Exception {
		Class<? extends T> proxyClass = new ByteBuddy().subclass(clazz).method(ElementMatchers.any())
				.intercept(InvocationHandlerAdapter.of(handler)).make().load(clazz.getClassLoader(),
						ClassLoadingStrategy.Default.INJECTION.with(PackageDefinitionStrategy.Trivial.INSTANCE))
				.getLoaded();

		return proxyClass.getConstructor().newInstance();
	}

}
