package fr.omny.odi.listener;

import java.lang.reflect.Parameter;

public interface OnConstructorCallListener {
	
	void newInstance(Object instance);

	Object value(Parameter parameter);

}
