package org.springframework.cloud.client.loadbalancer;

import java.io.IOException;
import java.net.URI;

/**
 * Exception to be thrown when the status code is deemed to be retryable.
 * @author Ryan Baxter
 */
public class RetryableStatusCodeException extends IOException {

	private static final String MESSAGE = "Service %s returned a status code of %d";

	private Object response;

	private URI uri;

	public RetryableStatusCodeException(String serviceId, int statusCode) {
		super(String.format(MESSAGE, serviceId, statusCode));
	}

	public RetryableStatusCodeException(String serviceId, int statusCode, Object response, URI uri) {
		super(String.format(MESSAGE, serviceId, statusCode));
		this.response = response;
		this.uri = uri;
	}

	public Object getResponse() {
		return response;
	}

	public URI getUri() {
		return uri;
	}
}
