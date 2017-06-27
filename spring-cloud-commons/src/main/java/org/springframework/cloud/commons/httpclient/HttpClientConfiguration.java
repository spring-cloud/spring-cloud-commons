package org.springframework.cloud.commons.httpclient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ryan Baxter
 */
@Configuration
public class HttpClientConfiguration {

	@Configuration
	@ConditionalOnProperty(name = "spring.cloud.httpclient.apache.enabled", matchIfMissing = true)
	static class ApacheHttpClientConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public ApacheHttpClientConnectionManagerFactory connManFactory() {
			return new DefaultApacheHttpClientConnectionManagerFactory();
		}

		@Bean
		@ConditionalOnMissingBean
		public ApacheHttpClientFactory apacheHttpClientFactory() {
			return new DefaultApacheHttpClientFactory();
		}
	}
}
