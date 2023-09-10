package com.mclarkdev.tools.libobjectpooler;

/**
 * LibObjectPooler // LibObjectPoolerBackoffException
 */
public class LibObjectPoolerBackoffException extends Exception {

	private static final long serialVersionUID = 1L;

	private static final int backoffMaxCount = 100;

	private final double backoffCount;
	private final double backoffMultiplier;
	private final Throwable backoffReason;

	/**
	 * Instantiate a new backoff execption.
	 * 
	 * @param count      number of backoffs received
	 * @param multiplier multiplier for the backoff
	 * @param e          the exception triggering the backoff
	 */
	public LibObjectPoolerBackoffException(double count, double multiplier, Exception e) {
		super(e);

		backoffReason = e;
		backoffMultiplier = multiplier;
		backoffCount = (count > backoffMaxCount) ? backoffMaxCount : count;
	}

	/**
	 * Returns the number of backoffs received.
	 * 
	 * @return number of backoffs received
	 */
	public double getBackoffCount() {

		return backoffCount;
	}

	/**
	 * Returns the reason for the backoff
	 * 
	 * @return reason for the backoff
	 */
	public Throwable getBackoffReason() {

		return backoffReason;
	}

	/**
	 * Returns the requested backoff delay
	 * 
	 * @return backoff delay
	 */
	public long getBackoffDelay() {

		return ((long) (100 * Math.pow(backoffCount, backoffMultiplier)));
	}
}
