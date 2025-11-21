/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.cloud.loadbalancer.core;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;

import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.AbstractServerHttpRequest;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * An {@link AbstractServerHttpRequest} implementation that is built from a
 * {@link RequestData} object. This is used to allow for passing a
 * {@code ServerWebExchange} to components that require it, but for which we only have
 * {@link RequestData} to construct it.
 *
 * <p>
 * Note: This is a lightweight implementation. Operations like {@link #getBody()} or
 * {@link #getNativeRequest()} are not supported and will throw an
 * {@link UnsupportedOperationException}.
 *
 * @author Olga Maciaszek-Sharma
 * @since 5.0.0
 * @see ReactiveApiVersionServiceInstanceListSupplier
 */
final class LoadBalancerServerHttpRequest extends AbstractServerHttpRequest {

	private final RequestData requestData;

	LoadBalancerServerHttpRequest(RequestData requestData) {
		super(requestData.getHttpMethod(), requestData.getUrl(), null,
				(requestData.getHeaders() != null) ? requestData.getHeaders() : new HttpHeaders());
		this.requestData = requestData;
	}

	@Override
	protected MultiValueMap<String, HttpCookie> initCookies() {
		MultiValueMap<String, HttpCookie> httpCookies = new LinkedMultiValueMap<>();
		MultiValueMap<String, String> originalCookies = requestData.getCookies();
		if (originalCookies != null) {
			originalCookies
				.forEach((name, values) -> values.forEach(value -> httpCookies.add(name, new HttpCookie(name, value))));
		}
		return httpCookies;
	}

	@Override
	protected @Nullable SslInfo initSslInfo() {
		return null;
	}

	@Override
	public <T> T getNativeRequest() {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public Flux<DataBuffer> getBody() {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

}
