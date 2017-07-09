package org.springframework.cloud.commons.httpclient;

import okhttp3.OkHttpClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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
	@ConditionalOnProperty(name = "spring.cloud.httpclientfactories.apache.enabled", matchIfMissing = true)
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

	@Configuration
	@ConditionalOnProperty(name = "spring.cloud.httpclientfactories.ok.enabled", matchIfMissing = true)
	@ConditionalOnClass(OkHttpClient.class)
	static class OkHttpClientConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public OkHttpClientConnectionPoolFactory connPoolFactory() {
			return new DefaultOkHttpClientConnectionPoolFactory();
		}

		@Bean
		@ConditionalOnMissingBean
		public OkHttpClientFactory okHttpClientFactory() {
			return new DefaultOkHttpClientFactory();
		}
	}
}
