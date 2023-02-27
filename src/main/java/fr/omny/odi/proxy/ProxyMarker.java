package fr.omny.odi.proxy;

public interface ProxyMarker {
	
	Class<?> getOriginalClass();

	Object getOriginalInstance();

}

