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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.OrderedHealthAggregator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.CommonsClientAutoConfiguration;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { DiscoveryCompositeHealthIndicatorTests.Config.class,
		CommonsClientAutoConfiguration.class })
public class DiscoveryCompositeHealthIndicatorTests {

	@Autowired
	DiscoveryCompositeHealthIndicator healthIndicator;

	@Configuration
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
					return new Health.Builder().up().build();
				}
			};
		}
	}

	@Test
	public void testHealthIndicator() {
		assertNotNull("healthIndicator was null", this.healthIndicator);
		Health health = this.healthIndicator.health();
		assertNotNull("health was null", health);
		Status status = health.getStatus();
		assertNotNull("status was null", status);
		assertEquals("status code was wrong", "UP", status.getCode());
		assertEquals("status desciption was wrong", "TestDiscoveryClient",
				status.getDescription());
	}

}
