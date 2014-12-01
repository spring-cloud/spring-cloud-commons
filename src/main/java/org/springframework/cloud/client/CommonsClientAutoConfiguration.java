package org.springframework.cloud.client;

import org.springframework.cloud.client.discovery.DiscoveryHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * @author Spencer Gibb
 */
@Configuration
@Order(0)
public class CommonsClientAutoConfiguration {

    @Bean
    public DiscoveryHealthIndicator discoveryHealthIndicator() {
        return new DiscoveryHealthIndicator();
    }
}
