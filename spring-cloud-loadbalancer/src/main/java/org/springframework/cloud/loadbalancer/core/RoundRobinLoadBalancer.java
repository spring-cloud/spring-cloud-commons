/*
 * Copyright 2013-2017 the original author or authors.
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

package org.springframework.cloud.loadbalancer.core;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.core.env.Environment;
import reactor.core.publisher.Mono;

import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * @author Spencer Gibb
 */
public class RoundRobinLoadBalancer implements LoadBalancer<ServiceInstance>, ReactiveLoadBalancer<ServiceInstance> {

	private static final Log log = LogFactory.getLog(RoundRobinLoadBalancer.class);

	private final AtomicInteger nextServerCyclicCounter = new AtomicInteger(-1);
	private final LoadBalancerClientFactory clientFactory;
	private final Environment environment;

	public RoundRobinLoadBalancer(LoadBalancerClientFactory clientFactory, Environment environment) {
		this.clientFactory = clientFactory;
		this.environment = environment;
	}

	@Override
	public LoadBalancer.Response<ServiceInstance> choose(LoadBalancer.Request request) {
		String serviceId = clientFactory.getName(this.environment);
		ServiceInstanceSupplier supplier = clientFactory.getInstance(serviceId, ServiceInstanceSupplier.class);
		List<ServiceInstance> instances = supplier.get();
		//TODO: enforce order?

		if (isEmpty(instances)) {
			log.warn("No servers available for service: " + serviceId);
			return new DefaultResponse(null);
		}

		int nextServerIndex = incrementAndGetModulo(instances.size());

		ServiceInstance instance = instances.get(nextServerIndex);

		return new DefaultResponse(instance);
	}

	@Override
	public Mono<ReactiveLoadBalancer.Response<ServiceInstance>> choose(ReactiveLoadBalancer.Request request) {
		return null;
	}

	/**
	 * Inspired by the implementation of {@link AtomicInteger#incrementAndGet()}.
	 * <p>
	 * original https://github.com/Netflix/ribbon/blob/master/ribbon-loadbalancer/src/main/java/com/netflix/loadbalancer/RoundRobinRule.java#L94-L107
	 *
	 * @param modulo The modulo to bound the value of the counter.
	 * @return The next value.
	 */
	private int incrementAndGetModulo(int modulo) {
		for (; ; ) {
			int current = nextServerCyclicCounter.get();
			int next = (current + 1) % modulo;
			if (nextServerCyclicCounter.compareAndSet(current, next))
				return next;
		}

	}
}
