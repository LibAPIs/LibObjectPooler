package com.mclarkdev.tools.libobjectpooler;

public class LibObjectPoolerLock {

	private long created;

	private boolean locked = false;
	private long lastLocked = 0;
	private long lockCount = 0;

	public LibObjectPoolerLock() {

		created = System.currentTimeMillis();
	}

	public long getCreated() {

		return created;
	}

	public boolean isLocked() {

		return locked;
	}

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

	public void unlock() {

		locked = false;
	}

	public long getLastLocked() {

		return lastLocked;
	}

	public long getLockCount() {

		return lockCount;
	}
}
