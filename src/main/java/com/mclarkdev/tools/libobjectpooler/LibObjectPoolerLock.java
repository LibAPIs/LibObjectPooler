package com.mclarkdev.tools.libobjectpooler;

/**
 * LibObjectPooler // LibObjectPoolerLock
 */
public class LibObjectPoolerLock {

	private long created;

	private boolean locked = false;
	private long lastLocked = 0;
	private long lockCount = 0;

	/**
	 * Instantiate a new Lock.
	 */
	public LibObjectPoolerLock() {

		created = System.currentTimeMillis();
	}

	/**
	 * Returns the time which the lock was created.
	 * 
	 * @return time lock created
	 */
	public long getCreated() {

		return created;
	}

	/**
	 * Returns true if the lock is currently locked.
	 * 
	 * @return is currently locked
	 */
	public boolean isLocked() {

		return locked;
	}

	/**
	 * Requests that the lock be locked.
	 * 
	 * @return locked successful
	 */
	public boolean lock() {

		synchronized (this) {

			if (locked) {
				return false;
			}

			locked = true;
			lastLocked = System.currentTimeMillis();
			lockCount++;

			return true;
		}
	}

	/**
	 * Requests that the lock be unlocked.
	 */
	public void unlock() {

		locked = false;
	}

	/**
	 * Get the time the lock was last locked.
	 * 
	 * @return time last locked
	 */
	public long getLastLocked() {

		return lastLocked;
	}

	/**
	 * Returns the number of times the lock has been locked.
	 * 
	 * @return number of locks
	 */
	public long getLockCount() {

		return lockCount;
	}
}
