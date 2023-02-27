package fr.omny.odi.proxy;

/**
 * Interface that need to be implemented by a proxy class
 */
public interface ProxyMarker {
	
	/**
	 * 
	 * @return the original class behind the proxy
	 */
	Class<?> getOriginalClass();

	/**
	 * 
	 * @return the instance behind the proxy
	 */
	Object getOriginalInstance();

}

