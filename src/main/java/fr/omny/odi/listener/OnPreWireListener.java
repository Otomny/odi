package fr.omny.odi.listener;

/**
 * Listen to pre-wire call
 */
public interface OnPreWireListener {

	/**
	 * Called when a object is going to be wire by the {@link fr.omny.odi.Injector}
	 * class
	 * 
	 * @param instance The object to be wire
	 */
	void wire(Object instance);

}
