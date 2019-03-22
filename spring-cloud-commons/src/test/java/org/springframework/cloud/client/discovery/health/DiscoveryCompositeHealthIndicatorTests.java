/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.cloud.client.discovery.health;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.OrderedHealthAggregator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.CommonsClientAutoConfiguration;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { DiscoveryCompositeHealthIndicatorTests.Config.class,
		CommonsClientAutoConfiguration.class })
public class DiscoveryCompositeHealthIndicatorTests {

	@Autowired
	private DiscoveryCompositeHealthIndicator healthIndicator;

	@Autowired
	private DiscoveryClientHealthIndicator clientHealthIndicator;

	@Test
	public void testHealthIndicator() {
		then(this.healthIndicator).as("healthIndicator was null").isNotNull();
		Health health = this.healthIndicator.health();
		assertHealth(health, Status.UNKNOWN);

		this.clientHealthIndicator
				.onApplicationEvent(new InstanceRegisteredEvent<>(this, null));

		health = this.healthIndicator.health();
		Status status = assertHealth(health, Status.UP);
		then("").isEqualTo(status.getDescription()).as("status description was wrong");
	}

	protected Status assertHealth(Health health, Status expected) {
		then(health).as("health was null").isNotNull();
		Status status = health.getStatus();
		then(status).as("status was null").isNotNull();
		then(expected.getCode()).isEqualTo(status.getCode()).as("status code was wrong");
		return status;
	}

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
					return new Health.Builder().unknown().build();
				}
			};
		}

	}

}
