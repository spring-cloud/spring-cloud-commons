package org.springframework.cloud.client;

import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.client.discovery.DiscoveryClientHealthIndicator;
import org.springframework.cloud.client.discovery.DiscoveryCompositeHealthIndicator;
import org.springframework.cloud.client.discovery.DiscoveryHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;

/**
 * @author Spencer Gibb
 */
@Configuration
@ConditionalOnClass(HealthIndicator.class)
@Order(0)
public class CommonsClientAutoConfiguration {

	@Bean
	public DiscoveryClientHealthIndicator instancesHealthIndicator() {
		return new DiscoveryClientHealthIndicator();
	}

    @Bean
    public DiscoveryCompositeHealthIndicator discoveryHealthIndicator(HealthAggregator aggregator, List<DiscoveryHealthIndicator> indicators) {
        return new DiscoveryCompositeHealthIndicator(aggregator, indicators);
    }
}
