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

package org.springframework.cloud.client.circuitbreaker.httpservice;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.client.CloudHttpClientServiceProperties;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;

/**
 * @author Olga Maciaszek-Sharma
 */
public class CircuitBreakerRestClientHttpServiceGroupConfigurer implements RestClientHttpServiceGroupConfigurer {

	// Make sure Boot's configurers run before
	private static final int ORDER = 15;

	private static final Log LOG = LogFactory.getLog(CircuitBreakerRestClientHttpServiceGroupConfigurer.class);

	private final CloudHttpClientServiceProperties clientServiceProperties;

	private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

	public CircuitBreakerRestClientHttpServiceGroupConfigurer(CloudHttpClientServiceProperties clientServiceProperties,
			CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
		this.clientServiceProperties = clientServiceProperties;
		this.circuitBreakerFactory = circuitBreakerFactory;
	}

	@Override
	public void configureGroups(Groups<RestClient.Builder> groups) {
		groups.forEachGroup((group, clientBuilder, factoryBuilder) -> {
			String groupName = group.name();
			CloudHttpClientServiceProperties.Group groupProperties = clientServiceProperties.getGroup().get(groupName);
			String fallbackClassName = (groupProperties != null) ? groupProperties.getFallbackClassName() : null;
			if (fallbackClassName == null || fallbackClassName.isBlank()) {
				return;
			}
			Class<?> fallbackClass = resolveFallbackClass(fallbackClassName);

			factoryBuilder.httpRequestValuesProcessor(new CircuitBreakerRequestValueProcessor());

			factoryBuilder.exchangeAdapterDecorator(httpExchangeAdapter -> new CircuitBreakerAdapterDecorator(
					httpExchangeAdapter, buildCircuitBreaker(groupName), fallbackClass));
		});
	}

	private Class<?> resolveFallbackClass(String className) {
		try {
			return Class.forName(className);
		}
		catch (ClassNotFoundException e) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Fallback class not found: " + className, e);
			}
			throw new IllegalStateException("Unable to load fallback class: " + className, e);
		}
	}

	private CircuitBreaker buildCircuitBreaker(String name) {
		return circuitBreakerFactory.create(name);
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

}
