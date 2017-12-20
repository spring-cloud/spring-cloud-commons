package org.springframework.cloud.client.loadbalancer.reactive;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;

/**
 * @author Spencer Gibb
 */
@Configuration
@ConditionalOnClass(WebClient.class)
@ConditionalOnBean(LoadBalancerClient.class)
public class ReactiveLoadBalancerAutoConfiguration {

	@LoadBalanced
	@Autowired(required = false)
	private List<WebClient.Builder> webClientBuilders = Collections.emptyList();

	public List<WebClient.Builder> getBuilders() {
		return webClientBuilders;
	}

	@Bean
	public SmartInitializingSingleton loadBalancedWebClientInitializer(
			final List<WebClientCustomizer> customizers) {
		return () -> {
			for (WebClient.Builder webClientBuilder : getBuilders()) {
				for (WebClientCustomizer customizer : customizers) {
					customizer.customize(webClientBuilder);
				}
			}
		};
	}

	@Bean
	public WebClientCustomizer loadbalanceClientWebClientCustomizer(LoadBalancerExchangeFilterFunction filterFunction) {
		return builder -> builder.filter(filterFunction);
	}

	@Bean
	public LoadBalancerExchangeFilterFunction loadBalancerExchangeFilterFunction(LoadBalancerClient client) {
		return new LoadBalancerExchangeFilterFunction(client);
	}
}
