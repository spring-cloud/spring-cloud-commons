package org.springframework.cloud.client.loadbalancer.reactive;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Spencer Gibb
 */
@Configuration
@ConditionalOnClass(WebClient.class)
@ConditionalOnBean(LoadBalancerClient.class)
public class ReactiveLoadBalancerAutoConfiguration {

	@Bean
	public LoadBalancerExchangeFilterFunction loadBalancerExchangeFilterFunction(LoadBalancerClient client) {
		return new LoadBalancerExchangeFilterFunction(client);
	}
}
