package com.mclarkdev.tools.libobjectpooler;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LibObjectPooler // LibObjectPooler
 * 
 * A generic object pool capable of managing and expiring old objects.
 * 
 * @param <T> the object type to pool
 */
public class LibObjectPooler<T> {

	private LibObjectPoolerController<T> controller;

	private Timer expireTimer = new Timer();

	private long expireCheckInterval = 15 * 1000;

	private boolean allowCreateNew = true;

	private int maxPoolSize;

	private long timeoutIdle = 0;
	private long maxAge = 0;
	private long maxLockCount = 0;
	private long maxLockTime = 0;

	private int backoffCount = 0;

	private ConcurrentHashMap<T, LibObjectPoolerLock> objectPool;

	/**
	 * Construct a new generic object pool.
	 * 
	 * @param maxPoolSize The initial maximum size of the pool; this can be changed
	 *                    at any time.
	 * @param controller  The controller to use for managing the lifecycle of the
	 *                    pooled objects.
	 */
	public LibObjectPooler(int maxPoolSize, LibObjectPoolerController<T> controller) {

		if (controller == null) {
			throw new IllegalArgumentException("controller cannot be null");
		}

		this.controller = controller;
		this.maxPoolSize = maxPoolSize;

		objectPool = new ConcurrentHashMap<T, LibObjectPoolerLock>();

		expireTimer.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {

				destroyExpiredObjects();
			}
		}, expireCheckInterval, expireCheckInterval);
	}

	/**
	 * Lock and get an object.
	 * 
	 * @return An instance of the object from the pool.
	 */
	public T get() throws LibObjectPoolerException {

		// loop each existing object
		for (T t : objectPool.keySet()) {

			// get the object lock
			LibObjectPoolerLock lock = objectPool.get(t);

			// if not locked, lock it
			if (!lock.isLocked() && lock.lock()) {

				// return the locked object
				return t;
			}
		}

		try {

			// return a new object
			T t = create();
			return t;

		} catch (LibObjectPoolerBackoffException e) {

			try {
				Thread.sleep(e.getBackoffDelay());
			} catch (InterruptedException ex) {
			}
			return get();
		}
	}

	/**
	 * Waits for an instance of a pooled object.
	 * 
	 * Uses default timeout (15s)
	 * 
	 * @return an instance of the pooled object
	 * @throws LibObjectPoolerException failed to get object before timeout
	 */
	public synchronized T getWait() throws LibObjectPoolerException {

		return getWait(15 * 1000);
	}

	/**
	 * Waits for an instance of a pooled object.
	 * 
	 * @param timeout the time to wait
	 * @return an instance of the pooled object
	 * @throws LibObjectPoolerException failed to get object before timeout
	 */
	public synchronized T getWait(long timeout) throws LibObjectPoolerException {

		long timeEnd = (timeout + System.currentTimeMillis());

		// wait for an object until timeout is reached
		while (timeEnd > System.currentTimeMillis()) {

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

		throw new LibObjectPoolerException("failed to get object before timeout");
	}

	/**
	 * Get the current size of the object pool.
	 * 
	 * @return The number of objects currently in the pool.
	 */
	public int getPoolSize() {

		// get the current size of the object pool
		return objectPool.size();
	}

	/**
	 * Get the max size of the object pool.
	 * 
	 * @return The maximum number of objects allowed in the pool.
	 */
	public int getMaxPoolSize() {

		// get the max size of the object pool
		return maxPoolSize;
	}

	/**
	 * Set the max size of the pool.
	 * 
	 * @param maxPoolSize The maximum number of objects allowed in the pool.
	 */
	public void setMaxPoolSize(int maxPoolSize) {

		// set the max pool size
		this.maxPoolSize = maxPoolSize;
	}

	/**
	 * Release a locked object.
	 * 
	 * @param t The locked object.
	 * @return Returns true if the object was successfully returned to the pool.
	 */
	public boolean release(T t) {

		if (t == null) {
			throw new IllegalArgumentException("cannot release null object");
		}

		synchronized (t) {

			// get the lock for the provided object
			LibObjectPoolerLock lock = objectPool.get(t);
			if (lock == null) {

				// false if object is not pooled
				return false;
			}

			// return successful unlock
			lock.unlock();
		}

		return true;
	}

	/**
	 * Get current number of locked objects.
	 * 
	 * @return The number of objects in the pool that are currently locked.
	 */
	public int getNumLocked() {

		int locked = 0;
		for (T t : objectPool.keySet()) {

			LibObjectPoolerLock lock = objectPool.get(t);
			if (lock.isLocked()) {

				locked++;
			}
		}

		return locked;
	}

	/**
	 * Get the current max object age.
	 * 
	 * @return The age of the oldest object in the pool.
	 */
	public long getMaxAge() {

		long oldestLock = 0;

		// loop each object
		for (T t : objectPool.keySet()) {

			// get the lock
			LibObjectPoolerLock lock = objectPool.get(t);

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
	 * @param maxAge Expire any objects which are older then the maximum allowable
	 *               age.
	 */
	public void setMaxAge(long maxAge) {

		this.maxAge = maxAge;
	}

	/**
	 * Get the current max object idle time.
	 * 
	 * @return The maximum idle time of all objects in the pool.
	 */
	public long getMaxIdle() {

		long oldestIdle = 0;

		// loop each object
		for (T t : objectPool.keySet()) {

			// get the lock
			LibObjectPoolerLock lock = objectPool.get(t);

			// skip if locked
			if (lock.isLocked()) {
				continue;
			}

			// get last lock time
			long locked = lock.getLastLocked();
			if (locked > 0) {

				// calculate idle time
				long idle = (System.currentTimeMillis() - locked);

				// compare to oldest
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
	 * @param timeoutIdle Expire any objects which have been idle for longer then
	 *                    the maximum allowed time.
	 */
	public void setMaxIdleTime(long timeoutIdle) {

		this.timeoutIdle = timeoutIdle;
	}

	/**
	 * Get the object which has been locked the most.
	 * 
	 * @return The number of times the object has been locked.
	 */
	public long getMaxLockCount() {

		long maxCount = 0;

		// loop each object
		for (T t : objectPool.keySet()) {

			// get the lock
			LibObjectPoolerLock lock = objectPool.get(t);

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

	/**
	 * Set the maximum number of times an object is allowed to be locked.
	 * 
	 * @param maxLockCount Expire any objects which have been locked more then the
	 *                     maximum allowable amount.
	 */
	public void setMaxLockCount(long maxLockCount) {

		this.maxLockCount = maxLockCount;
	}

	/**
	 * Set the maximum amount of time an object can remain locked before being
	 * forcibly destroyed.
	 * 
	 * @param maxLockTime Force expire objects which have been locked more then this
	 *                    amount of time.
	 */
	public void setMaxLockTime(long maxLockTime) {

		this.maxLockTime = maxLockTime;
	}

	/**
	 * Destroys a pooled object, if not locked.
	 * 
	 * @param t The object.
	 * @return Returns true if the object was destroyed from the pool.
	 */
	public boolean destroy(T t) {

		return destroy(t, false);
	}

	/**
	 * Destroy a pooled object, optionally with force.
	 * 
	 * @param t The object.
	 * @return Returns true if the object was destroyed from the pool.
	 */
	public boolean destroy(T t, boolean force) {

		if (t == null) {
			throw new IllegalArgumentException("cannot destroy null object");
		}

		synchronized (t) {

			// get the lock for the provided object
			LibObjectPoolerLock lock = objectPool.get(t);
			if (lock == null) {

				// false if object is not pooled
				return false;
			}

			// check that it is not locked
			if (!force && lock.isLocked()) {

				return false;
			}

			// remove from map
			objectPool.remove(t);

			try {

				// call implemented destroy
				controller.onDestroy(t);
			} catch (Exception | Error e) {

				// bubble up the failure
				throw new IllegalStateException("failed to destroy object", e);
			}
		}

		// return success
		return true;
	}

	/**
	 * Request that all items in the pool be destroyed.
	 * 
	 * @return number of items destroyed
	 */
	public synchronized int destroyAll() {

		int destroyed = 0;

		// loop each existing object
		for (T t : objectPool.keySet()) {

			// destroy it
			destroyed += destroy(t) ? 1 : 0;
		}

		// return the number destroyed
		return destroyed;
	}

	/**
	 * Shutdown the pooler and destroy all objects.
	 */
	public synchronized void shutdown() {

		// disallow new objects
		allowCreateNew = false;

		// stop the expire timer
		expireTimer.cancel();

		// destroy all existing
		destroyAll();
	}

	/**
	 * Destroy expired objects; this is called on a scheduled interval.
	 */
	public synchronized void destroyExpiredObjects() {

		// loop each object
		for (T t : objectPool.keySet()) {

			// get the lock
			LibObjectPoolerLock lock = objectPool.get(t);

			// calculate expiration times
			long timeNow = System.currentTimeMillis();
			long timeIdle = lock.getLastLocked() + timeoutIdle;
			long timeExpires = lock.getCreated() + maxAge;

			// check if past expiration timers
			boolean isExpiredIdle = ((timeoutIdle > 0) && (timeNow > timeIdle));
			boolean isExpiredAge = ((maxAge > 0) && (timeNow > timeExpires));
			boolean isExpired = (isExpiredIdle || isExpiredAge);

			// check if max lock count enabled and reached reached
			long lockCount = lock.getLockCount();
			boolean hitMaxLocks = ((maxLockCount > 0) && (lockCount > maxLockCount));

			// kill if max lock enabled and locked for too long
			long killAt = (lock.getLastLocked() + maxLockTime);
			boolean killable = ((maxLockTime > 0) && (killAt > timeNow));

			// determine if should be destroyed
			boolean destroy = (killable || isExpired || hitMaxLocks);

			// destroy the object
			if (destroy) {

				// attempt to destroy
				destroy(t, killable);
			}
		}
	}

	/**
	 * Create a new object instance.
	 * 
	 * @return The object.
	 * @throws LibObjectPoolerBackoffException
	 */
	private T create() throws LibObjectPoolerException, LibObjectPoolerBackoffException {

		// return null if the pool is full
		if (getPoolSize() >= maxPoolSize) {
			throw new LibObjectPoolerException("pool is at max capacity");
		}

		if (!allowCreateNew) {
			throw new LibObjectPoolerException("pool is not allowing new objects");
		}

		try {

			// create a new object
			T t = controller.onCreate();

			if (t == null) {
				throw new IllegalArgumentException("controller returned null object");
			}

			// reset backoff counter
			backoffCount = 0;

			// add to pool
			objectPool.put(t, new LibObjectPoolerLock());

			// return the object
			return t;
		} catch (Exception e) {

			throw new LibObjectPoolerBackoffException((++backoffCount), 2.0, e);
		}
	}
}
