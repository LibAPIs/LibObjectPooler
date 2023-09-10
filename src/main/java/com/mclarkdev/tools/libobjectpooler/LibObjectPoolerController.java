package com.mclarkdev.tools.libobjectpooler;

/**
 * LibObjectPooler // LibObjectPoolerController
 * 
 * @param <T> the object type to pool
 */
public interface LibObjectPoolerController<T> {

	/**
	 * Called by the pooler when a new object should be created.
	 * 
	 * @return the created object
	 */
	public T onCreate();

	/**
	 * Called by the pooler when an object should be destroyed.
	 * 
	 * @param t the object to destroy
	 */
	public void onDestroy(T t);
}
