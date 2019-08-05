package org.springframework.cloud.loadbalancer.config;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.client.loadbalancer.AsyncLoadBalancerAutoConfiguration;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * @author Olga Maciaszek-Sharma
 */
@Configuration
@LoadBalancerClients
@AutoConfigureAfter(AsyncLoadBalancerAutoConfiguration.class)
@AutoConfigureBefore({LoadBalancerAutoConfiguration.class,
		org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration.class})
@ConditionalOnBean(LoadBalancerClientFactory.class)
public class SpringLoadBalancerClientAutoConfiguration {

	@Bean
	@ConditionalOnClass(RestTemplate.class)
	public LoadBalancerClient loadBalancerClient(LoadBalancerClientFactory loadBalancerClientFactory) {
		return new org.springframework.cloud.loadbalancer.client.SpringLoadBalancerClient(loadBalancerClientFactory);
	}
}
