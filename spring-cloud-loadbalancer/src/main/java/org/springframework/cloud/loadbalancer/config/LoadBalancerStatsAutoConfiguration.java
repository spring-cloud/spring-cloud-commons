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

package org.springframework.cloud.loadbalancer.config;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.loadbalancer.stats.MicrometerStatsLoadBalancerLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Autoconfiguration that provides a {@link MicrometerStatsLoadBalancerLifecycle} bean.
 *
 * @author Olga Maciaszek-Sharma
 * @since 3.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnProperty(value = "spring.cloud.loadbalancer.stats.micrometer.enabled", havingValue = "true")
public class LoadBalancerStatsAutoConfiguration {

	@Bean
	@ConditionalOnBean(MeterRegistry.class)
	public MicrometerStatsLoadBalancerLifecycle micrometerStatsLifecycle(MeterRegistry meterRegistry) {
		return new MicrometerStatsLoadBalancerLifecycle(meterRegistry);
	}

}
