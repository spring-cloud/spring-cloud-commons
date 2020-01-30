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

import reactor.core.publisher.Mono;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * The default {@link InstanceHealthChecker} implementation that uses {@link WebClient} to
 * ping the <code>health</code> endpoint of the instances. Used by
 * {@link HealthCheckServiceInstanceListSupplier}.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.0
 * @see HealthCheckServiceInstanceListSupplier
 */
public class PingHealthChecker implements InstanceHealthChecker {

	private static final String LOADBALANCER_PREFIX = "spring.cloud.loadbalancer.";

	private static final String HEALTH_CHECK_SUFFIX = ".healthcheck.path";

	private static final String ACTUATOR_HEALTH = "/actuator/health";

	private final WebClient webClient;

	private final Environment environment;

	private final String defaultHealthCheckPath;

	public PingHealthChecker(Environment environment) {
		webClient = WebClient.builder().build();
		this.environment = environment;
		defaultHealthCheckPath = environment
				.getProperty(LOADBALANCER_PREFIX + HEALTH_CHECK_SUFFIX, ACTUATOR_HEALTH);
	}

	@Override
	public Mono<Boolean> isAlive(ServiceInstance serviceInstance) {
		String healthCheckPropertyValue = environment.getProperty(LOADBALANCER_PREFIX
				+ serviceInstance.getServiceId() + HEALTH_CHECK_SUFFIX);
		String healthCheckPath = healthCheckPropertyValue != null
				? healthCheckPropertyValue : defaultHealthCheckPath;
		return webClient.get()
				.uri(UriComponentsBuilder.fromUri(serviceInstance.getUri())
						.path(healthCheckPath).build().toUri())
				.exchange()
				.map(clientResponse -> HttpStatus.OK.equals(clientResponse.statusCode()));
	}

}
