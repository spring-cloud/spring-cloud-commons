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

package org.springframework.cloud.commons.util;

import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.health.AbstractReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link ReactiveHealthIndicator} that checks the configured {@link WebClient} for the
 * Spring Boot Health status format.
 * @author Spencer Gibb
 * @author Fabrizio Di Napoli
 */
public class UrlHealthIndicator extends AbstractReactiveHealthIndicator {

	private final WebClient webClient;

	public UrlHealthIndicator(WebClient webClient) {
		this.webClient = webClient;
	}

	public UrlHealthIndicator(WebClient.Builder builder, String baseUrl) {
		this.webClient = builder.baseUrl(baseUrl).build();
	}

	@Override
	protected Mono<Health> doHealthCheck(Health.Builder builder) {
		if (this.webClient == null) {
			return Mono.just(builder.up().build());
		}

		return webClient.get().retrieve().bodyToMono(Map.class).map(map -> {
			Object status = map.get("status");
			if (status instanceof String) {
				return builder.status(status.toString()).build();
			}
			else {
				return builder.unknown()
						.withDetail("warning", "no status field in response").build();
			}
		});
	}

}
