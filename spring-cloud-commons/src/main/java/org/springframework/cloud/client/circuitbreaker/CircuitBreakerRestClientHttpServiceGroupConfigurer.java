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

package org.springframework.cloud.client.circuitbreaker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.client.CloudHttpClientServiceProperties;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;

/**
 * @author Olga Maciaszek-Sharma
 */
public class CircuitBreakerRestClientHttpServiceGroupConfigurer implements RestClientHttpServiceGroupConfigurer {

	// Make sure Boot's configurers run before
	private static final int ORDER = 11;

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
			String fallbackClass = groupProperties == null ? null : groupProperties.getFallbackClass();
			factoryBuilder.httpRequestValuesProcessor(new CircuitBreakerRequestValueProcessor());
			Class<?> fallbacks;
			try {
				fallbacks = fallbackClass != null ? Class.forName(fallbackClass) : null;
			}
			catch (ClassNotFoundException e) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Could not load fallback class: " + fallbackClass, e);
				}
				throw new RuntimeException(e);
			}
			factoryBuilder.exchangeAdapterDecorator(httpExchangeAdapter -> new CircuitBreakerRestClientAdapterDecorator(
					httpExchangeAdapter, buildCircuitBreaker(resolveCircuitBreakerName(groupName)), fallbacks));
		});
	}

	// TODO
	private String resolveCircuitBreakerName(String groupName) {
		return groupName;
	}

	// TODO
	private CircuitBreaker buildCircuitBreaker(String circuitBreakerName) {
		return circuitBreakerFactory.create(circuitBreakerName);
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

}
