package org.springframework.cloud.commons.util;

import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Spencer Gibb
 */
@Configuration
@ConditionalOnProperty(value = "spring.cloud.util.enabled", matchIfMissing = true)
@AutoConfigureOrder(0)
@EnableConfigurationProperties
public class UtilAutoConfiguration {

	@Bean
	public InetUtilsProperties inetUtilsProperties() {
		return new InetUtilsProperties();
	}

	@Bean
	@ConditionalOnMissingBean
	public InetUtils inetUtils(InetUtilsProperties properties) {
		return new InetUtils(properties);
	}
}
