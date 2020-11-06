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

package org.springframework.cloud.client.loadbalancer.reactive;

import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Utility class for testing reactive load-balancer clients.
 *
 * @author Olga Maciaszek-Sharma
 */
final class LoadBalancerTestUtils {

	private LoadBalancerTestUtils() {
		throw new IllegalStateException("Can't instantiate a utility class");
	}

	static ConfigurableApplicationContext init(Class<?>... configClasses) {
		return new SpringApplicationBuilder().web(WebApplicationType.NONE)
				.sources(ArrayUtils.add(configClasses, WebClientAutoConfiguration.class)).run();
	}

	@SuppressWarnings("unchecked")
	static List<ExchangeFilterFunction> getFilters(WebClient.Builder builder) {
		return (List<ExchangeFilterFunction>) ReflectionTestUtils.getField(builder, "filters");
	}

	static void assertLoadBalanced(WebClient.Builder webClientBuilder, Class<?> exchangeFilterFunctionClass) {
		List<ExchangeFilterFunction> filters = getFilters(webClientBuilder);
		then(filters).hasSize(1);
		then(filters.get(0)).isInstanceOf(DeferringLoadBalancerExchangeFilterFunction.class);
		DeferringLoadBalancerExchangeFilterFunction interceptor = (DeferringLoadBalancerExchangeFilterFunction) filters
				.get(0);
		interceptor.tryResolveDelegate();
		then(interceptor.getDelegate()).isInstanceOf(exchangeFilterFunctionClass);
	}

	static void assertLoadBalanced(WebClient webClient, Class<?> exchangeFilterFunctionClass) {
		assertLoadBalanced((WebClient.Builder) ReflectionTestUtils.getField(webClient, "builder"),
				exchangeFilterFunctionClass);
	}

}
