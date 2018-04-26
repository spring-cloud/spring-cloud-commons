package org.springframework.cloud.commons.httpclient;

import okhttp3.OkHttpClient;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
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
	@ConditionalOnClass(HttpClient.class)
	static class ApacheHttpClientConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public ApacheHttpClientConnectionManagerFactory connManFactory() {
			return new DefaultApacheHttpClientConnectionManagerFactory();
		}

		@Bean
		@ConditionalOnMissingBean
		public HttpClientBuilder apacheHttpClientBuilder() {
			return HttpClientBuilder.create();
		}

		@Bean
		@ConditionalOnMissingBean
		public ApacheHttpClientFactory apacheHttpClientFactory(HttpClientBuilder builder) {
			return new DefaultApacheHttpClientFactory(builder);
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
		public OkHttpClient.Builder okHttpClientBuilder() {
			return new OkHttpClient.Builder();
		}

		@Bean
		@ConditionalOnMissingBean
		public OkHttpClientFactory okHttpClientFactory(OkHttpClient.Builder builder) {
			return new DefaultOkHttpClientFactory(builder);
		}
	}
}
