package org.aihac.utils.pooler;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple object pooler.
 * 
 * @author Matthew Clark
 *
 * @param <T>
 */
public class ObjectPooler<T> {

	private PooledObjectController<T> controller;

	private long expireCheckInterval = 15 * 1000;

	private int maxPoolSize;

	private long timeoutIdle = 0;
	private long maxAge = 0;
	private long maxLockCount = 0;

	private ConcurrentHashMap<T, PooledObjectLock> objectPool;

	/**
	 * Pooler constructor.
	 * 
	 * @param controller
	 * @param maxPoolSize
	 * @param idleTimeout
	 */
	public ObjectPooler(int maxPoolSize, PooledObjectController<T> controller) {

		this.controller = controller;
		this.maxPoolSize = maxPoolSize;

		objectPool = new ConcurrentHashMap<T, PooledObjectLock>();

		(new Timer()).scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {

				destroyExpiredObjects();
			}
		}, expireCheckInterval, expireCheckInterval);
	}

	/**
	 * Lock and get an object.
	 * 
	 * @return
	 */
	public synchronized T get() throws PooledObjectException {

		// loop each existing object
		for (T t : objectPool.keySet()) {

			// get the object lock
			PooledObjectLock lock = objectPool.get(t);

			// if not locked, lock it
			if (!lock.isLocked() && lock.lock()) {

				// return the locked object
				return t;
			}
		}

		// return a new object
		return create();
	}

	/**
	 * Wait for an object lock.
	 * 
	 * @return
	 */
	public synchronized T getWait() {

		// wait for an object
		while (true) {

			try {

				return get();
			} catch (Exception e) {

				try {
				
					// release some cycles
					Thread.sleep(1);
				} catch (Exception ex) {
				}
			}
		}
	}

	/**
	 * Get the current size of the pool.
	 * 
	 * @return
	 */
	public int getPoolSize() {

		// get the current size of the object pool
		return objectPool.size();
	}

	/**
	 * Get the max size of the pool.
	 * 
	 * @return
	 */
	public int getMaxPoolSize() {

		return maxPoolSize;
	}

	/**
	 * Set the max size of the pool.
	 * 
	 * @param maxPoolSize
	 */
	public void setMaxPoolSize(int maxPoolSize) {

		// set the max pool size
		this.maxPoolSize = maxPoolSize;
	}

	/**
	 * Release a locked object.
	 * 
	 * @param t
	 * @return
	 */
	public synchronized boolean release(T t) {

		// get the lock for the provided object
		PooledObjectLock lock = objectPool.get(t);
		if (lock == null) {

			// false if object is not pooled
			return false;
		}

		// return successful unlock
		return lock.unlock();
	}

	/**
	 * Get current number of locked objects.
	 * 
	 * @return
	 */
	public int getNumLocked() {

		int locked = 0;
		for (T t : objectPool.keySet()) {

			PooledObjectLock lock = objectPool.get(t);
			if (lock.isLocked()) {

				locked++;
			}
		}

		return locked;
	}

	/**
	 * Get the current max object age.
	 * 
	 * @return
	 */
	public long getMaxAge() {

		long oldestLock = 0;

		// loop each object
		for (T t : objectPool.keySet()) {

			// get the lock
			PooledObjectLock lock = objectPool.get(t);

			// calculate age
			long age = (System.currentTimeMillis() - lock.getCreated());

			// check if oldest
			if (age > oldestLock) {

				oldestLock = age;
			}
		}

		// return oldest lock
		return oldestLock;
	}

	/**
	 * Set the maximum allowed age of an object.
	 * 
	 * @param maxAge
	 */
	public void setMaxAge(long maxAge) {

		this.maxAge = maxAge;
	}

	/**
	 * Get the current max object idle time.
	 * 
	 * @return
	 */
	public long getMaxIdle() {

		long oldestIdle = 0;

		// loop each object
		for (T t : objectPool.keySet()) {

			// get the lock
			PooledObjectLock lock = objectPool.get(t);

			// calculate idle time
			long locked = lock.getLastLocked();
			if (locked > 0) {

				// calculate idle time
				long idle = (System.currentTimeMillis() - locked);

				// check if oldest
				if (idle > oldestIdle) {

					oldestIdle = idle;
				}
			}
		}

		// return oldest lock
		return oldestIdle;
	}

	/**
	 * Set the maximum allowed object idle time.
	 * 
	 * @param timeoutIdle
	 */
	public void setMaxIdleTime(long timeoutIdle) {

		this.timeoutIdle = timeoutIdle;
	}

	public long getMaxLockCount() {

		long maxCount = 0;

		// loop each object
		for (T t : objectPool.keySet()) {

			// get the lock
			PooledObjectLock lock = objectPool.get(t);

			// calculate idle time
			long lockCount = lock.getLockCount();

			// check if oldest
			if (lockCount > maxCount) {

				maxCount = lockCount;
			}
		}

		// return oldest lock
		return maxCount;
	}

	public void setMaxLockCount(long maxLockCount) {

		this.maxLockCount = maxLockCount;
	}

	/**
	 * Destroy a pooled object
	 * 
	 * @param t
	 * @return
	 */
	public synchronized boolean destroy(T t) {

		// get the lock for the provided object
		PooledObjectLock lock = objectPool.get(t);
		if (lock == null) {

			// false if object is not pooled
			return false;
		}

		// check that it is locked
		if (!lock.isLocked()) {

			return false;
		}

		// remove from map
		objectPool.remove(t);

		// call destroy
		controller.onDestroy(t);

		// return success
		return true;
	}

	/**
	 * Shutdown the pooler, destroying all objects.
	 */
	public synchronized void shutdown() {

		// loop each existing object
		for (T t : objectPool.keySet()) {

			// destroy it
			destroy(t);
		}
	}

	/**
	 * Destroy expired objects; this is called on a scheduled interval.
	 */
	public synchronized void destroyExpiredObjects() {

		// loop each object
		for (T t : objectPool.keySet()) {

			// get the lock
			PooledObjectLock lock = objectPool.get(t);

			// calculate expiration times
			long now = System.currentTimeMillis();
			long idle = lock.getLastLocked() + timeoutIdle;
			long expires = lock.getCreated() + maxAge;
			boolean expired = now > idle || now > expires;

			// check if max lock count reached
			long lockCount = lock.getLockCount();
			boolean hitMaxLocks = (maxLockCount > 0 && lockCount > maxLockCount);

			// check if should be destroyed
			if ((expired || hitMaxLocks) && lock.lock()) {

				// destroy
				destroy(t);
			}
		}
	}

	/**
	 * Create a new object instance.
	 * 
	 * @return
	 */
	private synchronized T create() throws PooledObjectException {

		// return null if the pool is full
		if (getPoolSize() >= maxPoolSize) {

			throw new PooledObjectException("pool is at max capacity");
		}

		// create a new object
		T t = controller.onCreate();
		PooledObjectLock lock = new PooledObjectLock();

		// add to pool
		objectPool.put(t, lock);

		// return
		return t;
	}
}
