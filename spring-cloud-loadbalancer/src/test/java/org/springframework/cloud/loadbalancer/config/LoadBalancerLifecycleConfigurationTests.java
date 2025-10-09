/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.cloud.loadbalancer.config;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycle;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LoadBalancerLifecycleConfiguration}.
 *
 * @author Jiwon Jeon
 */
class LoadBalancerLifecycleConfigurationTests {

	@Test
	void shouldInstantiateMultiAZFailoverLoadBalancerLifecycle() {
		baseApplicationRunner().withPropertyValues("spring.cloud.loadbalancer.configurations=multi-az-failover")
				.run(context -> {
					assertThat(context.getBeansOfType(LoadBalancerLifecycle.class)).hasSize(1);
					assertThat(context.getBean("multiAZFailoverLoadBalancerLifecycle"))
							.isInstanceOf(MultiAZFailoverLoadBalancerLifecycle.class);
				});
	}

	private ApplicationContextRunner baseApplicationRunner() {
		return new ApplicationContextRunner().withConfiguration(
				AutoConfigurations.of(LoadBalancerAutoConfiguration.class, LoadBalancerCacheAutoConfiguration.class,
						LoadBalancerCacheDataManagerConfiguration.class, LoadBalancerLifecycleConfiguration.class));
	}

}
