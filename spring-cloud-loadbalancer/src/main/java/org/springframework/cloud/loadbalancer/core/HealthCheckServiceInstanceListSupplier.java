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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.beans.factory.DisposableBean;
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
 * @author Roman Matiushchenko
 * @since 2.2.0
 */
public class HealthCheckServiceInstanceListSupplier
		implements ServiceInstanceListSupplier, DisposableBean {

	private static final Log LOG = LogFactory
			.getLog(HealthCheckServiceInstanceListSupplier.class);

	private final ServiceInstanceListSupplier delegate;

	private final LoadBalancerProperties.HealthCheck healthCheck;

	private final WebClient webClient;

	private final String defaultHealthCheckPath;

	private List<ServiceInstance> instances = Collections.emptyList();

	private volatile List<ServiceInstance> healthyInstances = Collections.emptyList();

	private Disposable healthCheckDisposable;

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
		healthCheckDisposable = delegate.get().doOnNext(delegateInstances -> {
			instances = Collections.unmodifiableList(new ArrayList<>(delegateInstances));
		})
		.thenMany(healthCheckFlux())
		.subscribeOn(Schedulers.parallel())
		.subscribe(verifiedInstances -> healthyInstances = verifiedInstances);
	}

	protected Flux<List<ServiceInstance>> healthCheckFlux() {
		return Flux.defer(() -> {
			List<Mono<ServiceInstance>> checks = new ArrayList<>(instances.size());
			for (ServiceInstance instance : instances) {
				Mono<ServiceInstance> alive = isAlive(instance)
						.onErrorResume(error -> {
							if (LOG.isDebugEnabled()) {
								LOG.debug(String.format(
										"Exception occurred during health check of the instance for service %s: %s",
										instance.getServiceId(), instance.getUri()), error);
							}
							return Mono.empty();
						})
						.timeout(healthCheck.getInterval(), Mono.fromSupplier(() -> {
							if (LOG.isDebugEnabled()) {
								LOG.debug(String.format(
										"The instance for service %s: %s did not respond for %s during health check",
										instance.getServiceId(), instance.getUri(), healthCheck.getInterval()));
							}
							//returns Completion signal if null
							return null;
						}))
						.handle((isHealthy, sink) -> {
							if (isHealthy) {
								sink.next(instance);
							}
						});

				checks.add(alive);
			}
			List<ServiceInstance> result = new CopyOnWriteArrayList<>();
			return Flux.merge(checks).map(alive -> {
				result.add(alive);
				return result;
			})
			.defaultIfEmpty(result);
		})
		.repeatWhen(restart -> restart.delayElements(healthCheck.getInterval()))
		.delaySubscription(Duration.ofMillis(healthCheck.getInitialDelay()));
	}

	@Override
	public String getServiceId() {
		return delegate.getServiceId();
	}

	@Override
	public Flux<List<ServiceInstance>> get() {
		return Flux.defer(() -> {
			List<ServiceInstance> it = new ArrayList<>(healthyInstances);
			if (it.isEmpty()) {
				if (LOG.isWarnEnabled()) {
					LOG.warn("No verified healthy instances were found, returning all listed instances.");
				}
				it = instances;
			}
			return Flux.just(it);
		});
	}

	protected Mono<Boolean> isAlive(ServiceInstance serviceInstance) {
		String healthCheckPropertyValue = healthCheck.getPath()
				.get(serviceInstance.getServiceId());
		String healthCheckPath = healthCheckPropertyValue != null
				? healthCheckPropertyValue : defaultHealthCheckPath;
		return webClient.get()
				.uri(UriComponentsBuilder.fromUri(serviceInstance.getUri())
						.path(healthCheckPath).build().toUri())
				.exchange()
				.flatMap(clientResponse -> clientResponse.releaseBody()
						.thenReturn(HttpStatus.OK.value() == clientResponse.rawStatusCode())
				);
	}

	@Override
	public void destroy() {
		this.healthCheckDisposable.dispose();
	}

}
