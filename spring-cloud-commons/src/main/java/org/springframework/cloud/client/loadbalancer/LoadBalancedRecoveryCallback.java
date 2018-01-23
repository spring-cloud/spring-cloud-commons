package org.springframework.cloud.client.loadbalancer;

import java.net.URI;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryException;

/**
 * @author Ryan Baxter
 */
public abstract class LoadBalancedRecoveryCallback<T, R> implements RecoveryCallback<T> {

	protected abstract T createResponse(R response, URI uri);

	@Override
	public T recover(RetryContext context) throws Exception {
		Throwable lastThrowable = context.getLastThrowable();
		if (lastThrowable != null && lastThrowable instanceof RetryableStatusCodeException) {
			RetryableStatusCodeException ex = (RetryableStatusCodeException) lastThrowable;
			return createResponse((R) ex.getResponse(), ex.getUri());
		}
		throw new RetryException("Could not recover", lastThrowable);
	}
}
