/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.cloud.loadbalancer.support.fixture;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A client-specific load balancer configuration. Deliberately has the same simple name as
 * the default configuration class
 * {@code org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientConfiguration}.
 * See gh-1359.
 *
 * @author akenra
 */
@Configuration(proxyBeanMethods = false)
public class LoadBalancerClientConfiguration {

	@Bean
	CustomFlag customFlag() {
		return new CustomFlag() {
		};
	}

	public interface CustomFlag {

	}

}
