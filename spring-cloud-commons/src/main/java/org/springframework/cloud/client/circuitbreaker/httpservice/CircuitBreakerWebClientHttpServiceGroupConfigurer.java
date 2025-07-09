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
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientHttpServiceGroupConfigurer;
import org.springframework.web.service.invoker.ReactorHttpExchangeAdapter;

import static org.springframework.cloud.client.circuitbreaker.httpservice.CircuitBreakerConfigurerUtils.resolveFallbackClass;

/**
 * @author Olga Maciaszek-Sharma
 */
public class CircuitBreakerWebClientHttpServiceGroupConfigurer implements WebClientHttpServiceGroupConfigurer {

	// Make sure Boot's configurers run before
	private static final int ORDER = 16;

	private static final Log LOG = LogFactory.getLog(CircuitBreakerWebClientHttpServiceGroupConfigurer.class);

	private final CloudHttpClientServiceProperties clientServiceProperties;

	private final ReactiveCircuitBreakerFactory<?, ?> reactiveCircuitBreakerFactory;

	private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

	public CircuitBreakerWebClientHttpServiceGroupConfigurer(CloudHttpClientServiceProperties clientServiceProperties,
			ReactiveCircuitBreakerFactory<?, ?> reactiveCircuitBreakerFactory,
			CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
		this.clientServiceProperties = clientServiceProperties;
		this.reactiveCircuitBreakerFactory = reactiveCircuitBreakerFactory;
		this.circuitBreakerFactory = circuitBreakerFactory;
	}

	@Override
	public void configureGroups(Groups<WebClient.Builder> groups) {
		groups.forEachGroup((group, clientBuilder, factoryBuilder) -> {
			String groupName = group.name();
			CloudHttpClientServiceProperties.Group groupProperties = clientServiceProperties.getGroup()
					.get(groupName);
			String fallbackClassName = (groupProperties != null) ? groupProperties.getFallbackClassName() : null;
			if (fallbackClassName == null || fallbackClassName.isBlank()) {
				return;
			}
			Class<?> fallbackClass = resolveFallbackClass(fallbackClassName);

			factoryBuilder.httpRequestValuesProcessor(new CircuitBreakerRequestValueProcessor());

			factoryBuilder.exchangeAdapterDecorator(httpExchangeAdapter -> {
				Assert.isInstanceOf(ReactorHttpExchangeAdapter.class, httpExchangeAdapter);
				return new ReactiveCircuitBreakerAdapterDecorator(
						(ReactorHttpExchangeAdapter) httpExchangeAdapter,
						buildReactiveCircuitBreaker(groupName), buildCircuitBreaker(groupName), fallbackClass);
			});
		});
	}

	private ReactiveCircuitBreaker buildReactiveCircuitBreaker(String groupName) {
		return reactiveCircuitBreakerFactory.create(groupName + "-reactive");
	}

	private CircuitBreaker buildCircuitBreaker(String groupName) {
		return circuitBreakerFactory.create(groupName);
	}

	@Override
	public int getOrder() {
		return ORDER;
	}
}
