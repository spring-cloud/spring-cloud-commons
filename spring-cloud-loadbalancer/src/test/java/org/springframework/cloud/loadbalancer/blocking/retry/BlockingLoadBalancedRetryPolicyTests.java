/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.cloud.loadbalancer.blocking.retry;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link BlockingLoadBalancedRetryPolicy}.
 *
 * @author Olga Maciaszek-Sharma
 */
class BlockingLoadBalancedRetryPolicyTests {

	private final BlockingLoadBalancerClient loadBalancerClient = mock(BlockingLoadBalancerClient.class);

	private final HttpRequest httpRequest = mock(HttpRequest.class);

	private final LoadBalancedRetryContext context = mock(LoadBalancedRetryContext.class);

	private final LoadBalancerProperties properties = new LoadBalancerProperties();

	private final UnsupportedOperationException exception = new UnsupportedOperationException();

	@BeforeEach
	void setUp() {
		when(httpRequest.getMethod()).thenReturn(HttpMethod.GET);
		when(context.getRequest()).thenReturn(httpRequest);
	}

	@Test
	void shouldExecuteIndicatedNumberOfSameAndNextInstanceRetriesAndCloseRetryContext() {
		properties.getRetry().setMaxRetriesOnSameServiceInstance(1);
		BlockingLoadBalancedRetryPolicy retryPolicy = getRetryPolicy(properties);

		assertThat(retryPolicy.canRetrySameServer(context)).isTrue();
		assertThat(retryPolicy.canRetryNextServer(context)).isTrue();

		retryPolicy.registerThrowable(context, exception);
		assertThat(retryPolicy.canRetrySameServer(context)).isFalse();
		assertThat(retryPolicy.canRetryNextServer(context)).isTrue();

		retryPolicy.registerThrowable(context, exception);
		assertThat(retryPolicy.canRetrySameServer(context)).isTrue();
		assertThat(retryPolicy.canRetryNextServer(context)).isTrue();

		retryPolicy.registerThrowable(context, exception);
		assertThat(retryPolicy.canRetrySameServer(context)).isFalse();
		assertThat(retryPolicy.canRetryNextServer(context)).isTrue();

		retryPolicy.registerThrowable(context, exception);
		verify(context).setExhaustedOnly();
		verify(context).setServiceInstance(any());
		assertThat(retryPolicy.canRetrySameServer(context)).isTrue();
		assertThat(retryPolicy.canRetryNextServer(context)).isFalse();
	}

	@Test
	void shouldNotRetryWhenMethodNotGet() {
		when(httpRequest.getMethod()).thenReturn(HttpMethod.POST);
		when(context.getRequest()).thenReturn(httpRequest);
		BlockingLoadBalancedRetryPolicy retryPolicy = getRetryPolicy(properties);

		boolean canRetry = retryPolicy.canRetry(context);

		assertThat(canRetry).isFalse();
	}

	@Test
	void shouldRetryOnPostWhenEnabled() {
		when(httpRequest.getMethod()).thenReturn(HttpMethod.POST);
		when(context.getRequest()).thenReturn(httpRequest);
		properties.getRetry().setRetryOnAllOperations(true);
		BlockingLoadBalancedRetryPolicy retryPolicy = getRetryPolicy(properties);

		boolean canRetry = retryPolicy.canRetry(context);

		assertThat(canRetry).isTrue();
	}

	@Test
	void shouldResolveRetryableStatusCode() {
		properties.getRetry().setRetryableStatusCodes(new HashSet<>(Arrays.asList(404, 502)));
		BlockingLoadBalancedRetryPolicy retryPolicy = getRetryPolicy(properties);

		boolean retryableStatusCode = retryPolicy.retryableStatusCode(404);

		assertThat(retryableStatusCode).isTrue();
	}

	private BlockingLoadBalancedRetryPolicy getRetryPolicy(LoadBalancerProperties properties) {
		return new BlockingLoadBalancedRetryPolicy(properties);
	}

}
