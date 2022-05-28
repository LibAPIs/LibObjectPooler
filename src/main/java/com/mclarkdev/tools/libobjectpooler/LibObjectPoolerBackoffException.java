package com.mclarkdev.tools.libobjectpooler;

public class LibObjectPoolerBackoffException extends Exception {

	private static final long serialVersionUID = 1L;

	private static final int backoffMaxCount = 100;

	private final double backoffCount;
	private final double backoffMultiplier;
	private final Throwable backoffReason;

	public LibObjectPoolerBackoffException(double count, double multiplier, Exception e) {
		super(e);

		backoffReason = e;
		backoffMultiplier = multiplier;
		backoffCount = (count > backoffMaxCount) ? backoffMaxCount : count;
	}

	public double getBackoffCount() {

		return backoffCount;
	}

	public Throwable getBackoffReason() {

		return backoffReason;
	}

	public long getBackoffDelay() {

		return ((long) (100 * Math.pow(backoffCount, backoffMultiplier)));
	}
}
