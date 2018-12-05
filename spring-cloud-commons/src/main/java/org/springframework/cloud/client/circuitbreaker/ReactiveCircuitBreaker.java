/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.client.circuitbreaker;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * @author Ryan Baxter
 */
public interface ReactiveCircuitBreaker {

	default public <T> Mono<T> run(Mono<T> toRun) {
		return run(toRun, null);
	}

	public <T> Mono<T> run(Mono<T> toRun, Function<Throwable, Mono<T>> fallback);

	default public <T> Flux<T> run(Flux<T> toRun) {
		return run(toRun, null);
	}

	public <T> Flux<T> run(Flux<T> toRun, Function<Throwable, Flux<T>> fallback);

}
