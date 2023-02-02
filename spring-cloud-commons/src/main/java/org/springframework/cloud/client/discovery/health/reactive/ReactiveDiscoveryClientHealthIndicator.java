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

package org.springframework.cloud.client.discovery.health.reactive;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
import org.springframework.cloud.client.discovery.health.DiscoveryClientHealthIndicatorProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;

import static java.util.Collections.emptyList;

/**
 * A health indicator which indicates whether the discovery client has been initialized.
 *
 * @author Tim Ysewyn
 * @author Chris Bono
 * @author Olga Maciaszek-Sharma
 */
public class ReactiveDiscoveryClientHealthIndicator
		implements ReactiveDiscoveryHealthIndicator, Ordered, ApplicationListener<InstanceRegisteredEvent<?>> {

	private static final Log LOG = LogFactory.getLog(ReactiveDiscoveryClientHealthIndicator.class);

	private final ReactiveDiscoveryClient discoveryClient;

	private final DiscoveryClientHealthIndicatorProperties properties;

	private AtomicBoolean discoveryInitialized = new AtomicBoolean(false);

	private int order = Ordered.HIGHEST_PRECEDENCE;

	public ReactiveDiscoveryClientHealthIndicator(ReactiveDiscoveryClient discoveryClient,
			DiscoveryClientHealthIndicatorProperties properties) {
		this.discoveryClient = discoveryClient;
		this.properties = properties;
	}

	@Override
	public void onApplicationEvent(InstanceRegisteredEvent<?> event) {
		if (discoveryInitialized.compareAndSet(false, true)) {
			LOG.debug("Discovery Client has been initialized");
		}
	}

	@Override
	public Mono<Health> health() {
		if (discoveryInitialized.get()) {
			return doHealthCheck();
		}
		else {
			return Mono.just(
					Health.status(new Status(Status.UNKNOWN.getCode(), "Discovery Client not initialized")).build());
		}
	}

	private Mono<Health> doHealthCheck() {
		// @formatter:off
		return Mono.just(properties.isUseServicesQuery())
				.flatMap(useServices -> useServices ? doHealthCheckWithServices() : doHealthCheckWithProbe())
				.onErrorResume(exception -> {
					if (LOG.isErrorEnabled()) {
						LOG.error("Error", exception);
					}
					return Mono.just(Health.down().withException(exception).build());
				});
		// @formatter:on
	}

	private Mono<Health> doHealthCheckWithProbe() {
		return discoveryClient.reactiveProbe().doOnError(exception -> {
			if (LOG.isErrorEnabled()) {
				LOG.error("Probe has failed.", exception);
			}
		}).then(buildHealthUp(discoveryClient));
	}

	private Mono<Health> buildHealthUp(ReactiveDiscoveryClient discoveryClient) {
		String description = (properties.isIncludeDescription()) ? discoveryClient.description() : "";
		return Mono.just(Health.status(new Status("UP", description)).build());
	}

	private Mono<Health> doHealthCheckWithServices() {
		// @formatter:off
		return Mono.justOrEmpty(discoveryClient)
				.flatMapMany(ReactiveDiscoveryClient::getServices)
				.collectList()
				.defaultIfEmpty(emptyList())
				.map(services -> {
					String description = (properties.isIncludeDescription()) ?
						discoveryClient.description() : "";
					return Health.status(new Status("UP", description))
						.withDetail("services", services).build();
				});
		// @formatter:on
	}

	@Override
	public String getName() {
		return discoveryClient.description();
	}

	@Override
	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

}
