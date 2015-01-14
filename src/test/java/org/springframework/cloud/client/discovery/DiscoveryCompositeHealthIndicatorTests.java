package org.springframework.cloud.client.discovery;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.OrderedHealthAggregator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.CommonsClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
			when(mock.description()).thenReturn("TestDiscoveryClient");
			when(mock.getServices()).thenReturn(Lists.newArrayList("TestService1"));
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
