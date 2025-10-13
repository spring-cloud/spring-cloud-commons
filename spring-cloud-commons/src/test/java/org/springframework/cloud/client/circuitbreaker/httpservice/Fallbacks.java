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

package org.springframework.cloud.client.circuitbreaker.httpservice;

import java.util.Collections;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;

/**
 * Test fallback class.
 *
 * @author Olga Maciaszek-Sharma
 */
public class Fallbacks {

	public void post(String test) {
		System.err.println("Fallback String: " + test);
	}

	public String test(String description, Integer value) {
		return description + ": " + value;
	}

	public String testThrowable(Throwable throwable, String description, Integer value) {
		return throwable + " " + description + ": " + value;
	}

	public Mono<String> testMono(String description, Integer value) {
		return Mono.just(description + ": " + value);
	}

	public Mono<String> testThrowableMono(Throwable throwable, String description, Integer value) {
		return Mono.just(throwable + " " + description + ": " + value);
	}

	public Flux<String> testFlux(String description, Integer value) {
		return Flux.just(description + ": " + value);
	}

	public Mono<HttpHeaders> testHttpHeadersMono(Throwable throwable, String description, Integer value) {
		return Mono.just(new HttpHeaders(
				MultiValueMap.fromSingleValue(Collections.singletonMap(description, String.valueOf(value)))));
	}

}
