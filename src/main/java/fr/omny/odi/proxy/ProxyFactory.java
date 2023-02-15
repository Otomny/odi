package fr.omny.odi.proxy;


import java.lang.reflect.InvocationHandler;

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
	public static <T> T newProxyInstance(Class<? extends T> clazz, InvocationHandler handler) throws Exception {
		Class<? extends T> proxyClass = new ByteBuddy().subclass(clazz).method(ElementMatchers.any())
				.intercept(InvocationHandlerAdapter.of(handler)).make().load(clazz.getClassLoader(),
						ClassLoadingStrategy.Default.INJECTION.with(PackageDefinitionStrategy.Trivial.INSTANCE))
				.getLoaded();

		return proxyClass.getConstructor().newInstance();
	}

}
