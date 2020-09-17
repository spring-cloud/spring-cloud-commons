/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.client.loadbalancer;

import java.io.IOException;
import java.net.URI;

/**
 * Exception to be thrown when the status code is deemed to be retryable.
 *
 * @author Ryan Baxter
 */
public class RetryableStatusCodeException extends IOException {

	private static final String MESSAGE = "Service %s returned a status code of %d";

	private final Object response;

	private final URI uri;

	public RetryableStatusCodeException(String serviceId, int statusCode, Object response, URI uri) {
		super(String.format(MESSAGE, serviceId, statusCode));
		this.response = response;
		this.uri = uri;
	}

	public Object getResponse() {
		return this.response;
	}

	public URI getUri() {
		return this.uri;
	}

}
