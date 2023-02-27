package fr.omny.odi.joinpoint;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import fr.omny.odi.Injector;
import fr.omny.odi.UnsafeUtils;
import fr.omny.odi.Utils;
import fr.omny.odi.listener.OnProxyCallListener;

public class JoinpointCallListener implements OnProxyCallListener {

	@Override
	public boolean pass(Method method) {
		return method.isAnnotationPresent(Pointcut.class);
	}

	@Override
	public Object invoke(Object instance, Method remoteMethod, Object[] arguments) throws Exception {
		Pointcut pointcutSetting = remoteMethod.getAnnotation(Pointcut.class);
		String name = pointcutSetting.value();
		if (name.equals("__methodName"))
			name = remoteMethod.getName();

		List<Method> joinPoints = Injector.getJoinpoints(instance.getClass(), name);
		if (joinPoints.isEmpty()) {
			return remoteMethod.invoke(instance, arguments);
		}
		if(Utils.isProxy(instance)){
			throw new IllegalStateException();
		}

		BiFunction<JoinpointPlace, List<Method>, List<Method>> callJoinPoints = (k, val) -> {
			val.forEach(method -> {
				try {
					Class<?> joinPointClass = method.getDeclaringClass();
					var joinPointInstance = Injector.getService(joinPointClass);
					if (joinPointInstance != null) {
						Utils.callMethod(method, joinPointClass, joinPointInstance,
								UnsafeUtils.concatenate(new Object[] { instance }, arguments));
					}
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					e.printStackTrace();
				}
			});
			return val;
		};

		Map<JoinpointPlace, List<Method>> jointPointsPerPlace = joinPoints.stream()
				.collect(Collectors.groupingBy(m -> m.getAnnotation(Joinpoint.class).place()));
		jointPointsPerPlace.computeIfPresent(JoinpointPlace.BEFORE_INVOKE, callJoinPoints);
		Object returnValue = remoteMethod.invoke(instance, arguments);
		jointPointsPerPlace.computeIfPresent(JoinpointPlace.AFTER_INVOKE, callJoinPoints);
		return returnValue;
	}

}
