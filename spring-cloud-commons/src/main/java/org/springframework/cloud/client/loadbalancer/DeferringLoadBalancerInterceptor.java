/*
 * Copyright 2012-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * An {@link ClientHttpRequestInterceptor} implementation that uses {@link ObjectProvider}
 * to resolve appropriate {@link BlockingLoadBalancerInterceptor} delegate when the
 * {@link ClientHttpRequestInterceptor#intercept(HttpRequest, byte[], ClientHttpRequestExecution)}
 * method is first called.
 *
 * @author Olga Maciaszek-Sharma
 * @since 4.1.2
 */
public class DeferringLoadBalancerInterceptor implements ClientHttpRequestInterceptor {

	private final ObjectProvider<BlockingLoadBalancerInterceptor> loadBalancerInterceptorProvider;

	private @Nullable BlockingLoadBalancerInterceptor delegate;

	public DeferringLoadBalancerInterceptor(
			ObjectProvider<BlockingLoadBalancerInterceptor> loadBalancerInterceptorProvider) {
		this.loadBalancerInterceptorProvider = loadBalancerInterceptorProvider;
	}

	@Override
	@SuppressWarnings("NullAway") // nullability checked in tryResolveDelegate()
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		tryResolveDelegate();
		return delegate.intercept(request, body, execution);
	}

	private void tryResolveDelegate() {
		if (delegate == null) {
			delegate = loadBalancerInterceptorProvider.getIfAvailable();
			if (delegate == null) {
				throw new IllegalStateException("LoadBalancer interceptor not available.");
			}
		}
	}

	// Visible for tests
	ObjectProvider<BlockingLoadBalancerInterceptor> getLoadBalancerInterceptorProvider() {
		return loadBalancerInterceptorProvider;
	}

}
