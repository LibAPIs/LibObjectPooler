package com.mclarkdev.tools.libobjectpooler;

public interface LibObjectPoolerController<T> {

	public T onCreate();

	public void onDestroy(T t);
}