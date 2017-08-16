package org.springframework.cloud.client.loadbalancer.impl;

import org.springframework.core.style.ToStringCreator;

/**
 * @author Spencer Gibb
 */
//TODO: add metrics
public class OnComplete {

	public enum Status {
		/** Request was handled successfully */
		SUCCESSS,
		/** Request reached the server but failed due to timeout or internal error */
		FAILED,
		/** Request did not go off box and should not be counted for statistics */
		DISCARD,
	}

	private final Status status;
	private final Throwable throwable;

	public OnComplete(Status status) {
		this(status, null);
	}

	public OnComplete(Status status, Throwable throwable) {
		this.status = status;
		this.throwable = throwable;
	}

	public Status getStatus() {
		return status;
	}

	public Throwable getThrowable() {
		return throwable;
	}

	@Override
	public String toString() {
		ToStringCreator to = new ToStringCreator(this);
		to.append("status", status);
		to.append("throwable", throwable);
		return to.toString();
	}

}
