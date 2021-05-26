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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.boot.convert.DurationStyle;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.core.env.Environment;

import static org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory.PROPERTY_NAME;

/**
 * A discovery-client-based {@link ServiceInstanceListSupplier} implementation.
 *
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 * @author Tim Ysewyn
 * @author Rod Catter
 * @since 2.2.0
 */
public class DiscoveryClientServiceInstanceListSupplier
		implements ServiceInstanceListSupplier {

	/**
	 * Property that establishes the timeout for calls to service discovery.
	 */
	public static final String SERVICE_DISCOVERY_TIMEOUT = "spring.cloud.loadbalancer.service-discovery.timeout";

	private static final Log LOG = LogFactory
			.getLog(DiscoveryClientServiceInstanceListSupplier.class);

	private Duration timeout = Duration.ofSeconds(30);

	private final String serviceId;

	private final Flux<List<ServiceInstance>> serviceInstances;

	public DiscoveryClientServiceInstanceListSupplier(DiscoveryClient delegate,
			Environment environment) {
		this.serviceId = environment.getProperty(PROPERTY_NAME);
		resolveTimeout(environment);
		this.serviceInstances = Flux
				.defer(() -> Mono.fromCallable(() -> delegate.getInstances(serviceId)))
				.timeout(timeout, Flux.defer(() -> {
					logTimeout();
					return Flux.just(new ArrayList<>());
				}), Schedulers.boundedElastic()).onErrorResume(error -> {
					logException(error);
					return Flux.just(new ArrayList<>());
				});
	}

	public DiscoveryClientServiceInstanceListSupplier(ReactiveDiscoveryClient delegate,
			Environment environment) {
		this.serviceId = environment.getProperty(PROPERTY_NAME);
		resolveTimeout(environment);
		this.serviceInstances = Flux.defer(() -> delegate.getInstances(serviceId)
				.collectList().flux().timeout(timeout, Flux.defer(() -> {
					logTimeout();
					return Flux.just(new ArrayList<>());
				})).onErrorResume(error -> {
					logException(error);
					return Flux.just(new ArrayList<>());
				}));
	}

	@Override
	public String getServiceId() {
		return serviceId;
	}

	@Override
	public Flux<List<ServiceInstance>> get() {
		return serviceInstances;
	}

	private void resolveTimeout(Environment environment) {
		String providedTimeout = environment.getProperty(SERVICE_DISCOVERY_TIMEOUT);
		if (providedTimeout != null) {
			timeout = DurationStyle.detectAndParse(providedTimeout);
		}
	}

	private void logTimeout() {
		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format(
					"Timeout occurred while retrieving instances for service %s."
							+ "The instances could not be retrieved during %s",
					serviceId, timeout));
		}
	}

	private void logException(Throwable error) {
		LOG.error(String.format(
				"Exception occurred while retrieving instances for service %s",
				serviceId), error);
	}

}
