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

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

/**
 * An {@link ExchangeFilterFunction} implementation that uses {@link ObjectProvider} to
 * resolve appropriate load-balancing {@link ExchangeFilterFunction} delegate when the
 * {@link ExchangeFilterFunction#filter(ClientRequest, ExchangeFunction)} method is first
 * called.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.0
 */
public class DeferringLoadBalancerExchangeFilterFunction<T extends ExchangeFilterFunction>
		implements ExchangeFilterFunction {

	private final ObjectProvider<T> exchangeFilterFunctionProvider;

	private T delegate;

	public DeferringLoadBalancerExchangeFilterFunction(
			ObjectProvider<T> exchangeFilterFunctionProvider) {
		this.exchangeFilterFunctionProvider = exchangeFilterFunctionProvider;
	}

	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
		tryResolveDelegate();
		return delegate.filter(request, next);
	}

	// Visible for tests
	void tryResolveDelegate() {
		if (delegate == null) {
			delegate = exchangeFilterFunctionProvider.getIfAvailable();
			if (delegate == null) {
				throw new IllegalStateException(
						"ReactorLoadBalancerExchangeFilterFunction not available.");
			}
		}
	}

	// Visible for tests
	T getDelegate() {
		return delegate;
	}

}
