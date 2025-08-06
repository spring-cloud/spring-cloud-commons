/*
 * Copyright 2012-2025 the original author or authors.
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

// FIXME if FW API not adjusted to use the request alone
/**
 * @author Olga Maciaszek-Sharma
 */
class EmptyServerHttpResponse implements ServerHttpResponse {


	@Override
	public boolean setStatusCode(@Nullable HttpStatusCode status) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public @Nullable HttpStatusCode getStatusCode() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public void addCookie(ResponseCookie cookie) {

	}

	@Override
	public DataBufferFactory bufferFactory() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public void beforeCommit(Supplier<? extends Mono<Void>> action) {

	}

	@Override
	public boolean isCommitted() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public Mono<Void> setComplete() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public HttpHeaders getHeaders() {
		throw new UnsupportedOperationException("Please, implement me.");
	}
}
