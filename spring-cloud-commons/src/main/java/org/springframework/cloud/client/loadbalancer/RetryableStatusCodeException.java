package org.springframework.cloud.client.loadbalancer;

import java.io.IOException;

/**
 * Exception to be thrown when the status code is deemed to be retryable.
 * @author Ryan Baxter
 */
public class RetryableStatusCodeException extends IOException {

	private static final String MESSAGE = "Service %s returned a status code of %d";

	public RetryableStatusCodeException(String serviceId, int statusCode) {
		super(String.format(MESSAGE, serviceId, statusCode));
	}
}
