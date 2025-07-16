/*
 * Copyright 2013-2025 the original author or authors.
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

package org.springframework.cloud.client;

import org.springframework.cloud.client.circuitbreaker.httpservice.CircuitBreakerAdapterDecorator;
import org.springframework.cloud.client.circuitbreaker.httpservice.ReactiveCircuitBreakerAdapterDecorator;

/**
 * Spring Cloud-specific {@code HttpClientServiceProperties}.
 *
 * @author Olga Maciaszek-Sharma
 * @since 5.0.0
 */
public abstract class AbstractCloudHttpClientServiceProperties {

	/**
	 * Name of the class that contains fallback methods to be called by
	 * {@link CircuitBreakerAdapterDecorator} or
	 * {@link ReactiveCircuitBreakerAdapterDecorator} in case a fallback is triggered.
	 * <p>
	 * Both the fallback class and the fallback methods must be public.
	 * </p>
	 */
	private String fallbackClassName;

	public String getFallbackClassName() {
		return fallbackClassName;
	}

	public void setFallbackClassName(String fallbackClassName) {
		this.fallbackClassName = fallbackClassName;
	}

}
