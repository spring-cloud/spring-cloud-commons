package org.springframework.cloud.client;

import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.client.discovery.DiscoveryHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * @author Spencer Gibb
 */
@Configuration
@ConditionalOnClass(HealthIndicator.class)
@Order(0)
public class CommonsClientAutoConfiguration {

    @Bean
    public DiscoveryHealthIndicator discoveryHealthIndicator() {
        return new DiscoveryHealthIndicator();
    }
}
