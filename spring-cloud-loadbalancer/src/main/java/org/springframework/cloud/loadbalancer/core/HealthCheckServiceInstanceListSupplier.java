/*
 * Copyright 2012-2021 the original author or authors.
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
import java.util.function.BiFunction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.retry.Repeat;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.util.StringUtils;

/**
 * A {@link ServiceInstanceListSupplier} implementation that verifies whether the
 * instances are alive and only returns the healthy one, unless there are none. Uses a
 * user-provided function to ping the <code>health</code> endpoint of the instances.
 *
 * @author Olga Maciaszek-Sharma
 * @author Roman Matiushchenko
 * @author Roman Chigvintsev
 * @since 2.2.0
 */
public class HealthCheckServiceInstanceListSupplier extends DelegatingServiceInstanceListSupplier
		implements InitializingBean, DisposableBean {

	private static final Log LOG = LogFactory.getLog(HealthCheckServiceInstanceListSupplier.class);

	private final LoadBalancerProperties.HealthCheck healthCheck;

	private final String defaultHealthCheckPath;

	private final Flux<List<ServiceInstance>> aliveInstancesReplay;

	private Disposable healthCheckDisposable;

	private final BiFunction<ServiceInstance, String, Mono<Boolean>> aliveFunction;

	/**
	 * @deprecated in favour of
	 * {@link HealthCheckServiceInstanceListSupplier#HealthCheckServiceInstanceListSupplier(ServiceInstanceListSupplier, ReactiveLoadBalancer.Factory, BiFunction)}
	 */
	@Deprecated
	public HealthCheckServiceInstanceListSupplier(ServiceInstanceListSupplier delegate,
			LoadBalancerProperties.HealthCheck healthCheck,
			BiFunction<ServiceInstance, String, Mono<Boolean>> aliveFunction) {
		super(delegate);
		defaultHealthCheckPath = healthCheck.getPath().getOrDefault("default", "/actuator/health");
		this.aliveFunction = aliveFunction;
		this.healthCheck = healthCheck;
		Repeat<Object> aliveInstancesReplayRepeat = Repeat
				.onlyIf(repeatContext -> this.healthCheck.getRefetchInstances())
				.fixedBackoff(healthCheck.getRefetchInstancesInterval());
		Flux<List<ServiceInstance>> aliveInstancesFlux = Flux.defer(delegate).repeatWhen(aliveInstancesReplayRepeat)
				.switchMap(serviceInstances -> healthCheckFlux(serviceInstances)
						.map(alive -> Collections.unmodifiableList(new ArrayList<>(alive))));
		aliveInstancesReplay = aliveInstancesFlux.delaySubscription(healthCheck.getInitialDelay()).replay(1)
				.refCount(1);
	}

	public HealthCheckServiceInstanceListSupplier(ServiceInstanceListSupplier delegate,
			ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerClientFactory,
			BiFunction<ServiceInstance, String, Mono<Boolean>> aliveFunction) {
		super(delegate);
		this.healthCheck = loadBalancerClientFactory.getProperties(getServiceId()).getHealthCheck();
		defaultHealthCheckPath = healthCheck.getPath().getOrDefault("default", "/actuator/health");
		this.aliveFunction = aliveFunction;
		Repeat<Object> aliveInstancesReplayRepeat = Repeat
				.onlyIf(repeatContext -> this.healthCheck.getRefetchInstances())
				.fixedBackoff(healthCheck.getRefetchInstancesInterval());
		Flux<List<ServiceInstance>> aliveInstancesFlux = Flux.defer(delegate).repeatWhen(aliveInstancesReplayRepeat)
				.switchMap(serviceInstances -> healthCheckFlux(serviceInstances)
						.map(alive -> Collections.unmodifiableList(new ArrayList<>(alive))));
		aliveInstancesReplay = aliveInstancesFlux.delaySubscription(healthCheck.getInitialDelay()).replay(1)
				.refCount(1);
	}

	@Override
	public void afterPropertiesSet() {
		Disposable healthCheckDisposable = this.healthCheckDisposable;
		if (healthCheckDisposable != null) {
			healthCheckDisposable.dispose();
		}
		this.healthCheckDisposable = aliveInstancesReplay.subscribe();
	}

	protected Flux<List<ServiceInstance>> healthCheckFlux(List<ServiceInstance> instances) {
		Repeat<Object> healthCheckFluxRepeat = Repeat.onlyIf(repeatContext -> healthCheck.getRepeatHealthCheck())
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
								instance.getServiceId(), instance.getUri(), healthCheck.getInterval()));
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
		boolean containsService = healthCheck.getPath().containsKey(serviceInstance.getServiceId());
		String healthCheckPropertyValue = healthCheck.getPath().get(serviceInstance.getServiceId());
		if (containsService && !StringUtils.hasText(healthCheckPropertyValue)) {
			return Mono.just(true);
		}
		String healthCheckPath = healthCheckPropertyValue != null ? healthCheckPropertyValue : defaultHealthCheckPath;
		return aliveFunction.apply(updatedServiceInstance(serviceInstance), healthCheckPath);
	}

	@Override
	public void destroy() {
		Disposable healthCheckDisposable = this.healthCheckDisposable;
		if (healthCheckDisposable != null) {
			healthCheckDisposable.dispose();
		}
	}

	private ServiceInstance updatedServiceInstance(ServiceInstance serviceInstance) {
		Integer healthCheckPort = healthCheck.getPort();
		if (serviceInstance instanceof DefaultServiceInstance && healthCheckPort != null) {
			return new DefaultServiceInstance(serviceInstance.getInstanceId(), serviceInstance.getServiceId(),
					serviceInstance.getHost(), healthCheckPort, serviceInstance.isSecure(),
					serviceInstance.getMetadata());
		}
		return serviceInstance;
	}

}
