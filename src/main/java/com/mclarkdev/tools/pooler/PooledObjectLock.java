package com.mclarkdev.tools.pooler;

public class PooledObjectLock {

	private long created;

	private boolean locked = false;
	private long lastLocked = 0;
	private long lockCount = 0;

	public PooledObjectLock() {

		created = System.currentTimeMillis();
	}

	public long getCreated() {

		return created;
	}

	public boolean isLocked() {

		return locked;
	}

	public boolean lock() {

		locked = true;
		lastLocked = System.currentTimeMillis();
		lockCount++;

		return locked;
	}

	public boolean unlock() {

		locked = false;
		return locked;
	}

	public long getLastLocked() {

		return lastLocked;
	}

	public long getLockCount() {

		return lockCount;
	}
}
