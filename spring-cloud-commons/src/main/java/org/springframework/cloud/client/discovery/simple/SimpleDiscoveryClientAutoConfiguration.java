package org.springframework.cloud.client.discovery.simple;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.noop.NoopDiscoveryClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot Auto-Configuration for Simple Properties based Discovery Client
 *
 * @author Biju Kunjummen
 */

@Configuration
@ConditionalOnProperty(value = "spring.cloud.discovery.client.simple.enabled", matchIfMissing = false)
@EnableConfigurationProperties(SimpleDiscoveryProperties.class)
@AutoConfigureBefore(NoopDiscoveryClientAutoConfiguration.class)
public class SimpleDiscoveryClientAutoConfiguration {

	@Autowired
	private SimpleDiscoveryProperties simpleDiscoveryProperties;

	@Bean
	public DiscoveryClient simpleDiscoveryClient() {
		return new SimpleDiscoveryClient(simpleDiscoveryProperties);
	}
}
