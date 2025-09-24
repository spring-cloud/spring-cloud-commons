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

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MultiValueMap;

/**
 * A dummy {@link ServerHttpResponse} implementation that allows for integration with
 * Spring Framework's API versioning features.
 *
 * @author Olga Maciaszek-Sharma
 * @since 5.0.0
 */
class EmptyServerHttpResponse implements ServerHttpResponse {

	private static final String ERROR_MESSAGE = "Not supported by " + EmptyServerHttpResponse.class.getSimpleName();

	@Override
	public boolean setStatusCode(@Nullable HttpStatusCode status) {
		throw new UnsupportedOperationException(ERROR_MESSAGE);
	}

	@Override
	public @Nullable HttpStatusCode getStatusCode() {
		throw new UnsupportedOperationException(ERROR_MESSAGE);
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		throw new UnsupportedOperationException(ERROR_MESSAGE);
	}

	@Override
	public void addCookie(ResponseCookie cookie) {
		throw new UnsupportedOperationException(ERROR_MESSAGE);
	}

	@Override
	public DataBufferFactory bufferFactory() {
		throw new UnsupportedOperationException(ERROR_MESSAGE);
	}

	@Override
	public void beforeCommit(Supplier<? extends Mono<Void>> action) {
		throw new UnsupportedOperationException(ERROR_MESSAGE);
	}

	@Override
	public boolean isCommitted() {
		throw new UnsupportedOperationException(ERROR_MESSAGE);
	}

	@Override
	public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		throw new UnsupportedOperationException(ERROR_MESSAGE);
	}

	@Override
	public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		throw new UnsupportedOperationException(ERROR_MESSAGE);
	}

	@Override
	public Mono<Void> setComplete() {
		throw new UnsupportedOperationException(ERROR_MESSAGE);
	}

	@Override
	public HttpHeaders getHeaders() {
		throw new UnsupportedOperationException(ERROR_MESSAGE);
	}

}
