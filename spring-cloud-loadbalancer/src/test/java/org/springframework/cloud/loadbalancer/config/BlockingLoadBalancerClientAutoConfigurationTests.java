/*
 * Copyright 2013-2020 the original author or authors.
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
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 * @author Tim Ysewyn
 */
class BlockingLoadBalancerClientAutoConfigurationTests {

	private ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(LoadBalancerAutoConfiguration.class,
					BlockingLoadBalancerClientAutoConfiguration.class));

	@Test
	void beansCreatedNormally() {
		applicationContextRunner.run(ctxt -> {
			assertThat(ctxt).hasSingleBean(BlockingLoadBalancerClient.class);
			assertThat(ctxt).hasSingleBean(LoadBalancedRetryFactory.class);
		});
	}

	@Test
	public void worksWithoutSpringWeb() {
		applicationContextRunner.withClassLoader(new FilteredClassLoader(RestTemplate.class)).run(context -> {
			assertThat(context).doesNotHaveBean(BlockingLoadBalancerClient.class);
		});
	}

	@Test
	void shouldNotFailOnRetryFactoryWhenLoadBalancingDisabled() {
		applicationContextRunner.withPropertyValues("spring.cloud.loadbalancer.enabled=false").run(context -> {
			assertThat(context).doesNotHaveBean(BlockingLoadBalancerClient.class);
			assertThat(context).doesNotHaveBean(LoadBalancedRetryFactory.class);
		});
	}

}
