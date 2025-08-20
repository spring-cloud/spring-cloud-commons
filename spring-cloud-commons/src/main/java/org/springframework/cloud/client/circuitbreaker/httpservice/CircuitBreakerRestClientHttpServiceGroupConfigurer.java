/*
 * Copyright 2013-present the original author or authors.
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

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;

import static org.springframework.cloud.client.circuitbreaker.httpservice.CircuitBreakerConfigurerUtils.resolveAnnotatedFallbackClasses;

/**
 * An implementation of {@link RestClientHttpServiceGroupConfigurer} that provides
 * CircuitBreaker integration for configured groups. This configurer applies
 * CircuitBreaker logic to each HTTP service group and provides fallback behavior based on
 * the {@link HttpServiceFallback} annotations configured by the user.
 *
 * @author Olga Maciaszek-Sharma
 * @since 5.0.0
 * @see HttpServiceFallback
 */
public class CircuitBreakerRestClientHttpServiceGroupConfigurer
		implements RestClientHttpServiceGroupConfigurer, ApplicationContextAware {

	// Make sure Boot's configurers run before
	private static final int ORDER = 15;

	private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

	private ApplicationContext applicationContext;

	public CircuitBreakerRestClientHttpServiceGroupConfigurer(CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
		this.circuitBreakerFactory = circuitBreakerFactory;
	}

	@Override
	public void configureGroups(Groups<RestClient.Builder> groups) {
		groups.forEachGroup((group, clientBuilder, factoryBuilder) -> {
			String groupName = group.name();
			Map<String, Class<?>> perGroupFallbackClasses = resolveAnnotatedFallbackClasses(applicationContext,
					groupName);
			Map<String, Class<?>> fallbackClasses = !perGroupFallbackClasses.isEmpty() ? perGroupFallbackClasses
					: resolveAnnotatedFallbackClasses(applicationContext, null);
			factoryBuilder.httpRequestValuesProcessor(new CircuitBreakerRequestValueProcessor());

			factoryBuilder
				.exchangeAdapterDecorator(httpExchangeAdapter -> new CircuitBreakerAdapterDecorator(httpExchangeAdapter,
						buildCircuitBreaker(groupName), fallbackClasses));
		});
	}

	private CircuitBreaker buildCircuitBreaker(String groupName) {
		return circuitBreakerFactory.create(groupName);
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

}
