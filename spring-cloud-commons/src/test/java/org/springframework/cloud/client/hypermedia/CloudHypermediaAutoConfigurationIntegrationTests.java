/*
 * Copyright 2015-2016 the original author or authors.
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
package org.springframework.cloud.client.hypermedia;

import org.junit.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.hypermedia.CloudHypermediaAutoConfiguration.CloudHypermediaProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.client.Traverson;
import org.springframework.hateoas.client.Traverson.TraversalBuilder;

import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Integration tests for {@link CloudHypermediaAutoConfiguration}.
 *
 * @author Oliver Gierke
 */
public class CloudHypermediaAutoConfigurationIntegrationTests {

	@Test
	public void picksUpHypermediaProperties() {

		try (ConfigurableApplicationContext context = getApplicationContext(
				ConfigWithRemoteResource.class)) {

			CloudHypermediaProperties properties = context
					.getBean(CloudHypermediaProperties.class);

			assertThat(properties.getRefresh().getInitialDelay(), is(50000));
			assertThat(properties.getRefresh().getFixedDelay(), is(10000));
		}
	}

	@Test
	public void doesNotCreateCloudHypermediaPropertiesifNotActive() {

		try (ConfigurableApplicationContext context = getApplicationContext(
				Config.class)) {
			assertThat(context.getBeanNamesForType(CloudHypermediaProperties.class),
					is(arrayWithSize(0)));
		}
	}

	@Test
	public void doesNotRegisterResourceRefresherIfNoDiscoveredResourceIsDefined() {

		try (ConfigurableApplicationContext context = getApplicationContext(
				Config.class)) {

			assertThat(context.getBeansOfType(RemoteResource.class).values(), hasSize(0));
			assertThat(context.getBeanNamesForType(RemoteResourceRefresher.class),
					is(arrayWithSize(0)));
		}
	}

	@Test
	public void registersResourceRefresherIfDiscoverredResourceIsDefined() {

		try (ConfigurableApplicationContext context = getApplicationContext(
				ConfigWithRemoteResource.class)) {

			assertThat(context.getBeansOfType(RemoteResource.class).values(), hasSize(1));
			assertThat(context.getBean(RemoteResourceRefresher.class),
					is(notNullValue()));
		}
	}

	private static ConfigurableApplicationContext getApplicationContext(
			Class<?> configuration) {
		return new SpringApplicationBuilder(configuration).properties("server.port=0")
				.run();
	}

	@Configuration
	@EnableAutoConfiguration
	static class Config {
	}

	@Configuration
	@EnableAutoConfiguration
	static class ConfigWithRemoteResource {

		@Bean
		public RemoteResource resource() {

			ServiceInstanceProvider provider = new StaticServiceInstanceProvider(
					new DefaultServiceInstance("service", "localhost", 80, false));
			return new DiscoveredResource(provider, new TraversalDefinition() {

				@Override
				public TraversalBuilder buildTraversal(Traverson traverson) {
					return traverson.follow("rel");
				}
			});
		}
	}
}
