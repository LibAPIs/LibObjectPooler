package org.aihac.utils.pooler;

public interface PooledObjectController<T> {

	public T onCreate();

	public void onDestroy(T t);
}