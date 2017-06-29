/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.client.discovery.health;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.OrderedHealthAggregator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.CommonsClientAutoConfiguration;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { DiscoveryClientHealthIndicatorTests.Config.class,
		CommonsClientAutoConfiguration.class }, properties = "spring.cloud.discovery.client.health-indicator.include-description:true")
public class DiscoveryClientHealthIndicatorTests {

	@Autowired
	private DiscoveryCompositeHealthIndicator healthIndicator;

	@Autowired
	private DiscoveryClientHealthIndicator clientHealthIndicator;

	@Configuration
	@EnableConfigurationProperties
	public static class Config {
		@Bean
		public HealthAggregator healthAggregator() {
			return new OrderedHealthAggregator();
		}

		@Bean
		public DiscoveryClient discoveryClient() {
			DiscoveryClient mock = mock(DiscoveryClient.class);
			given(mock.description()).willReturn("TestDiscoveryClient");
			given(mock.getServices()).willReturn(Arrays.asList("TestService1"));
			return mock;
		}

		@Bean
		public DiscoveryHealthIndicator discoveryHealthIndicator() {
			return new DiscoveryHealthIndicator() {
				@Override
				public String getName() {
					return "testDiscoveryHealthIndicator";
				}

				@Override
				public Health health() {
					return new Health.Builder().unknown().build();
				}
			};
		}
	}

	@Test
	public void testHealthIndicatorDescriptionDisabled() {
		assertNotNull("healthIndicator was null", this.healthIndicator);
		Health health = this.healthIndicator.health();
		assertHealth(health, Status.UNKNOWN);

		clientHealthIndicator.onApplicationEvent(new InstanceRegisteredEvent<>(this, null));

		health = this.healthIndicator.health();
		Status status = assertHealth(health, Status.UP);
		assertEquals("status description was wrong", "TestDiscoveryClient",
				status.getDescription());
	}

	private Status assertHealth(Health health, Status expected) {
		assertNotNull("health was null", health);
		Status status = health.getStatus();
		assertNotNull("status was null", status);
		assertEquals("status code was wrong", expected.getCode(), status.getCode());
		return status;
	}
}
