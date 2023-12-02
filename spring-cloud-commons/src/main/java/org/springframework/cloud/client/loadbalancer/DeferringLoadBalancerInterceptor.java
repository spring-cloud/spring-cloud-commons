/*
 * Copyright 2012-2023 the original author or authors.
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

import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * An {@link ClientHttpRequestInterceptor} implementation that uses
 * {@link ApplicationContext} to resolve appropriate load-balancing
 * {@link ClientHttpRequestInterceptor} delegate when the
 * {@link ClientHttpRequestInterceptor#intercept(HttpRequest, byte[], ClientHttpRequestExecution)}
 * method is first called.
 *
 * @author Freeman Lau
 * @since 4.1.0
 */
public class DeferringLoadBalancerInterceptor implements ClientHttpRequestInterceptor {

	private final ApplicationContext ctx;

	private ClientHttpRequestInterceptor delegate;

	DeferringLoadBalancerInterceptor(ApplicationContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		tryResolveDelegate();
		return delegate.intercept(request, body, execution);
	}

	void tryResolveDelegate() {
		if (delegate == null) {
			delegate = ctx.getBeanProvider(LoadBalancedInterceptor.class).getIfAvailable();
			if (delegate == null) {
				throw new IllegalStateException("LoadBalancedInterceptor not available.");
			}
		}
	}

}
