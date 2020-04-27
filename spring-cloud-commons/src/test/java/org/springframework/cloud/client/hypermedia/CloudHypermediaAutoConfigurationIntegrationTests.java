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

package org.springframework.cloud.client.hypermedia;

import org.junit.Test;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.hypermedia.CloudHypermediaAutoConfiguration.CloudHypermediaProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Integration tests for {@link CloudHypermediaAutoConfiguration}.
 *
 * @author Oliver Gierke
 * @author Tim Ysewyn
 */
public class CloudHypermediaAutoConfigurationIntegrationTests {

	private static ConfigurableApplicationContext getApplicationContext(
			Class<?> configuration) {
		return new SpringApplicationBuilder(configuration).properties("server.port=0")
				.run();
	}

	@Test
	public void picksUpHypermediaProperties() {

		try (ConfigurableApplicationContext context = getApplicationContext(
				ConfigWithRemoteResource.class)) {

			CloudHypermediaProperties properties = context
					.getBean(CloudHypermediaProperties.class);

			then(properties.getRefresh().getInitialDelay()).isEqualTo(50000);
			then(properties.getRefresh().getFixedDelay()).isEqualTo(10000);
		}
	}

	@Test
	public void doesNotCreateCloudHypermediaPropertiesifNotActive() {

		try (ConfigurableApplicationContext context = getApplicationContext(
				Config.class)) {
			then(context.getBeanNamesForType(CloudHypermediaProperties.class)).hasSize(0);
		}
	}

	@Test
	public void doesNotRegisterResourceRefresherIfNoDiscoveredResourceIsDefined() {

		try (ConfigurableApplicationContext context = getApplicationContext(
				Config.class)) {

			then(context.getBeansOfType(RemoteResource.class).values()).hasSize(0);
			then(context.getBeanNamesForType(RemoteResourceRefresher.class)).hasSize(0);
		}
	}

	@Test
	public void registersResourceRefresherIfDiscoverredResourceIsDefined() {

		try (ConfigurableApplicationContext context = getApplicationContext(
				ConfigWithRemoteResource.class)) {

			then(context.getBeansOfType(RemoteResource.class).values()).hasSize(1);
			then(context.getBean(RemoteResourceRefresher.class)).isNotNull();
		}
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	static class Config {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	static class ConfigWithRemoteResource {

		@Bean
		public RemoteResource resource() {

			ServiceInstanceProvider provider = new StaticServiceInstanceProvider(
					new DefaultServiceInstance("instance", "service", "localhost", 80,
							false));
			return new DiscoveredResource(provider, traverson -> traverson.follow("rel"));
		}

	}

}
