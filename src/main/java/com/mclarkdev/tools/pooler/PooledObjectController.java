package com.mclarkdev.tools.pooler;

public interface PooledObjectController<T> {

	public T onCreate();

	public void onDestroy(T t);
}