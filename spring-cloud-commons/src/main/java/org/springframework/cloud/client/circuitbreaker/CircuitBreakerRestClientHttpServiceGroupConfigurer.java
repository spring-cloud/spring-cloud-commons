package org.springframework.cloud.client.circuitbreaker;

import org.springframework.cloud.client.CloudHttpClientServiceProperties;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;

/**
 * @author Olga Maciaszek-Sharma
 */
public class CircuitBreakerRestClientHttpServiceGroupConfigurer implements RestClientHttpServiceGroupConfigurer {

	// Make sure Boot's configurers run before
	private static final int ORDER = 11;

	private final CloudHttpClientServiceProperties clientServiceProperties;

	public CircuitBreakerRestClientHttpServiceGroupConfigurer(CloudHttpClientServiceProperties clientServiceProperties) {
		this.clientServiceProperties = clientServiceProperties;
	}

	@Override
	public void configureGroups(Groups<RestClient.Builder> groups) {
		groups.forEachGroup((group, clientBuilder, factoryBuilder) -> {
			String groupName = group.name();
			CloudHttpClientServiceProperties.Group groupProperties = clientServiceProperties.getGroup()
					.get(groupName);
			String fallbackClass = groupProperties == null ? null : groupProperties.getFallbackClass();
			factoryBuilder.httpRequestValuesProcessor(new CircuitBreakerRequestValueProcessor());
			Class<?> fallbacks = null;
			try {
				fallbacks = Class.forName(fallbackClass);
			}
			catch (ClassNotFoundException e) {
				// TODO
				throw new RuntimeException(e);
			}
			// TODO: change to decorator
			factoryBuilder.exchangeAdapter(CircuitBreakerRestClientAdapter.create(RestClient.builder()
					.build(), buildCircuitBreaker(), fallbacks));
		});
	}


	private CircuitBreaker buildCircuitBreaker() {
		return null;
	}


	@Override
	public int getOrder() {
		return ORDER;
	}
}
