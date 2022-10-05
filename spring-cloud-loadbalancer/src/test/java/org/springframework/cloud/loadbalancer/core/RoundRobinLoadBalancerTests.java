/*
 * Copyright 2012-2022 the original author or authors.
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

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.support.SimpleObjectProvider;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Zhuozhi JI
 */
class RoundRobinLoadBalancerTests {

	@Test
	void shouldOrderEnforcedWhenPositive() {
		assertOrderEnforced(0);
	}

	@Test
	void shouldOrderEnforcedWhenNegative() {
		assertOrderEnforced(MIN_VALUE);
	}

	@Test
	void shouldOrderEnforcedWhenPositiveOverflow() {
		assertOrderEnforced(MAX_VALUE);
	}

	@Test
	void shouldNotMovePositionIfOnlyOneInstance() {
		ServiceInstanceListSupplier supplier = mock(ServiceInstanceListSupplier.class);
		when(supplier.get(any())).thenReturn(Flux.just(Collections.singletonList(new DefaultServiceInstance())));
		RoundRobinLoadBalancer loadBalancer = new RoundRobinLoadBalancer(new SimpleObjectProvider<>(supplier),
				"shouldNotMovePositionIfOnlyOneInstance", 0);

		loadBalancer.choose().block();
		assertThat(loadBalancer.position).hasValue(0);

		loadBalancer.choose().block();
		assertThat(loadBalancer.position).hasValue(0);
	}

	@SuppressWarnings("all")
	void assertOrderEnforced(int seed) {
		List<ServiceInstance> instances = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			ServiceInstance instance = mock(ServiceInstance.class);
			when(instance.getInstanceId()).thenReturn(i + "");
			instances.add(instance);
		}
		SameInstancePreferenceServiceInstanceListSupplier supplier = mock(
				SameInstancePreferenceServiceInstanceListSupplier.class);
		when(supplier.get(any())).thenReturn(Flux.just(instances));

		RoundRobinLoadBalancer loadBalancer = new RoundRobinLoadBalancer(new SimpleObjectProvider<>(supplier),
				"shouldStartFromZeroWhenPositiveOverflow", seed);

		for (int i = 0; i < 10; i++) {
			int instanceId = ((seed + 1 + i) & MAX_VALUE) % instances.size();
			ServiceInstance chosen = loadBalancer.choose().block().getServer();
			assertThat(chosen.getInstanceId()).isEqualTo(instanceId + "");
		}
	}

}
