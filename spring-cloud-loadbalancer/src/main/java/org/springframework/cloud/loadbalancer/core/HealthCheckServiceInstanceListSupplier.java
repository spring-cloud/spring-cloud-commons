/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerProperties;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * A {@link ServiceInstanceListSupplier} implementation that verifies whether the
 * instances are alive and only returns the healthy one, unless there are none. Uses
 * {@link WebClient} to ping the <code>health</code> endpoint of the instances.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.0
 */
public class HealthCheckServiceInstanceListSupplier
		implements ServiceInstanceListSupplier {

	private static final Log LOG = LogFactory
			.getLog(HealthCheckServiceInstanceListSupplier.class);

	private final ServiceInstanceListSupplier delegate;

	private final LoadBalancerProperties.HealthCheck healthCheck;

	private final WebClient webClient;

	private final String defaultHealthCheckPath;

	private List<ServiceInstance> instances = Collections
			.synchronizedList(new ArrayList<>());

	private List<ServiceInstance> healthyInstances = Collections
			.synchronizedList(new ArrayList<>());

	public HealthCheckServiceInstanceListSupplier(ServiceInstanceListSupplier delegate,
			LoadBalancerProperties.HealthCheck healthCheck, WebClient webClient) {
		this.delegate = delegate;
		this.healthCheck = healthCheck;
		defaultHealthCheckPath = healthCheck.getPath().getOrDefault("default",
				"/actuator/health");
		this.webClient = webClient;
		initInstances();

	}

	private void initInstances() {
		delegate.get().subscribe(delegateInstances -> {
			instances.clear();
			instances.addAll(delegateInstances);
		});

		Flux<List<ServiceInstance>> healthCheckFlux = healthCheckFlux();

		healthCheckFlux.subscribe(verifiedInstances -> {
			healthyInstances.clear();
			healthyInstances.addAll(verifiedInstances);
		});
	}

	protected Flux<List<ServiceInstance>> healthCheckFlux() {
		return Flux.create(emitter -> Schedulers
				.newSingle("Health Check Verifier: " + getServiceId(), true)
				.schedulePeriodically(() -> {
					List<ServiceInstance> verifiedInstances = new ArrayList<>();
					Flux.fromIterable(instances).filterWhen(this::isAlive)
							.subscribe(serviceInstance -> {
								verifiedInstances.add(serviceInstance);
								emitter.next(verifiedInstances);
							});
				}, healthCheck.getInitialDelay(), healthCheck.getInterval().toMillis(),
						TimeUnit.MILLISECONDS),
				FluxSink.OverflowStrategy.LATEST);
	}

	@Override
	public String getServiceId() {
		return delegate.getServiceId();
	}

	@Override
	public Flux<List<ServiceInstance>> get() {
		if (!healthyInstances.isEmpty()) {
			return Flux.defer(() -> Flux.fromIterable(healthyInstances).collectList());
		}
		// If there are no healthy instances, it might be better to still retry on all of
		// them
		if (LOG.isWarnEnabled()) {
			LOG.warn("No verified healthy instances were found, returning all listed instances.");
		}
		return Flux.defer(() -> Flux.fromIterable(instances).collectList());
	}

	// Visible for tests
	Mono<Boolean> isAlive(ServiceInstance serviceInstance) {
		String healthCheckPropertyValue = healthCheck.getPath()
				.get(serviceInstance.getServiceId());
		String healthCheckPath = healthCheckPropertyValue != null
				? healthCheckPropertyValue : defaultHealthCheckPath;
		return webClient.get()
				.uri(UriComponentsBuilder.fromUri(serviceInstance.getUri())
						.path(healthCheckPath).build().toUri())
				.exchange()
				.map(clientResponse -> HttpStatus.OK.equals(clientResponse.statusCode()));
	}

}
