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

package org.springframework.cloud.loadbalancer.core;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.retry.Repeat;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
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
 * @author Roman Chigvintsev
 * @since 2.2.0
 */
public class HealthCheckServiceInstanceListSupplier
		extends DelegatingServiceInstanceListSupplier
		implements InitializingBean, DisposableBean {

	private static final Log LOG = LogFactory
			.getLog(HealthCheckServiceInstanceListSupplier.class);

	private final LoadBalancerProperties.HealthCheck healthCheck;

	private final WebClient webClient;

	private final String defaultHealthCheckPath;

	private final Flux<List<ServiceInstance>> aliveInstancesReplay;

	private Disposable healthCheckDisposable;

	public HealthCheckServiceInstanceListSupplier(ServiceInstanceListSupplier delegate,
			LoadBalancerProperties.HealthCheck healthCheck, WebClient webClient) {
		super(delegate);
		defaultHealthCheckPath = healthCheck.getPath().getOrDefault("default",
				"/actuator/health");
		this.webClient = webClient;
		this.healthCheck = healthCheck;
		Repeat<Object> aliveInstancesReplayRepeat = Repeat
				.onlyIf(repeatContext -> this.healthCheck.getRefetchInstances())
				.fixedBackoff(healthCheck.getRefetchInstancesInterval());
		Flux<List<ServiceInstance>> aliveInstancesFlux = Flux.defer(delegate)
				.repeatWhen(aliveInstancesReplayRepeat)
				.switchMap(serviceInstances -> healthCheckFlux(serviceInstances).map(
						alive -> Collections.unmodifiableList(new ArrayList<>(alive))));
		aliveInstancesReplay = aliveInstancesFlux
				.delaySubscription(Duration.ofMillis(healthCheck.getInitialDelay()))
				.replay(1).refCount(1);
	}

	@Override
	public void afterPropertiesSet() {
		Disposable healthCheckDisposable = this.healthCheckDisposable;
		if (healthCheckDisposable != null) {
			healthCheckDisposable.dispose();
		}
		this.healthCheckDisposable = aliveInstancesReplay.subscribe();
	}

	protected Flux<List<ServiceInstance>> healthCheckFlux(
			List<ServiceInstance> instances) {
		Repeat<Object> healthCheckFluxRepeat = Repeat
				.onlyIf(repeatContext -> healthCheck.getRepeatHealthCheck())
				.fixedBackoff(healthCheck.getInterval());
		return Flux.defer(() -> {
			List<Mono<ServiceInstance>> checks = new ArrayList<>(instances.size());
			for (ServiceInstance instance : instances) {
				Mono<ServiceInstance> alive = isAlive(instance).onErrorResume(error -> {
					if (LOG.isDebugEnabled()) {
						LOG.debug(String.format(
								"Exception occurred during health check of the instance for service %s: %s",
								instance.getServiceId(), instance.getUri()), error);
					}
					return Mono.empty();
				}).timeout(healthCheck.getInterval(), Mono.defer(() -> {
					if (LOG.isDebugEnabled()) {
						LOG.debug(String.format(
								"The instance for service %s: %s did not respond for %s during health check",
								instance.getServiceId(), instance.getUri(),
								healthCheck.getInterval()));
					}
					return Mono.empty();
				})).handle((isHealthy, sink) -> {
					if (isHealthy) {
						sink.next(instance);
					}
				});

				checks.add(alive);
			}
			List<ServiceInstance> result = new ArrayList<>();
			return Flux.merge(checks).map(alive -> {
				result.add(alive);
				return result;
			}).defaultIfEmpty(result);
		}).repeatWhen(healthCheckFluxRepeat);
	}

	@Override
	public Flux<List<ServiceInstance>> get() {
		return aliveInstancesReplay;
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
				.flatMap(clientResponse -> clientResponse.releaseBody().thenReturn(
						HttpStatus.OK.value() == clientResponse.rawStatusCode()));
	}

	@Override
	public void destroy() {
		Disposable healthCheckDisposable = this.healthCheckDisposable;
		if (healthCheckDisposable != null) {
			healthCheckDisposable.dispose();
		}
	}

}
