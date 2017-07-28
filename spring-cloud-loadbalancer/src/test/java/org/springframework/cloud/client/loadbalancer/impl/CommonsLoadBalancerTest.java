package org.springframework.cloud.client.loadbalancer.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.impl.scoped.CachedDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.impl.scoped.ScopedDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.impl.scoped.ScopedLoadBalancer;
import org.springframework.cloud.client.loadbalancer.impl.scoped.RandomScopedLoadBalancer;
import org.springframework.cloud.client.loadbalancer.impl.scoped.RoundRobinScopedLoadBalancer;
import org.springframework.cloud.client.loadbalancer.impl.annotation.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.impl.annotation.LoadBalancerClientSpecification;
import org.springframework.cloud.client.loadbalancer.impl.annotation.LoadBalancerClients;
import org.springframework.cloud.client.loadbalancer.impl.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class CommonsLoadBalancerTest {

	@Autowired
	private CommonsLoadBalancer loadBalancer;
	
	@Autowired
	private LoadBalancerClientFactory clientFactory;

	@Test
	public void roundRobbinLoadbalancerWorks() {
		ScopedLoadBalancer scopedLoadBalancer = this.clientFactory.getInstance("myservice", ScopedLoadBalancer.class);

		assertThat(scopedLoadBalancer).isInstanceOf(RoundRobinScopedLoadBalancer.class);

		List<String> hosts = Arrays.asList("a.host", "c.host", "b.host", "a.host");

		for (String host : hosts) {
			ServiceInstance instance = this.loadBalancer.choose("myservice");
			assertThat(instance).isNotNull();
			assertThat(instance.getHost()).isEqualTo(host).as("instance host is incorrent %s", host);
			if (host.equals("b.host")) {
				assertThat(instance.isSecure()).isTrue();
			} else {
				assertThat(instance.isSecure()).isFalse();
			}
		}
	}

	@Test
	public void randomAlgorithmWorks() {
		ScopedLoadBalancer scopedLoadBalancer = this.clientFactory.getInstance("anotherservice", ScopedLoadBalancer.class);

		assertThat(scopedLoadBalancer).isInstanceOf(RandomScopedLoadBalancer.class);

		List<String> hosts = Arrays.asList("d.host", "e.host", "f.host");
		for (int i=0; i < 10; i++) {
			ServiceInstance instance = this.loadBalancer.choose("anotherservice");
			assertThat(instance).isNotNull();
			assertThat(instance.getHost()).isIn(hosts).as("instance host is incorrent %s", instance.getHost());
		}
	}

	@Test
	public void decorateDiscoveryClientWorks() {
		ScopedDiscoveryClient discoveryClient = this.clientFactory.getInstance("thirdservice", ScopedDiscoveryClient.class);

		assertThat(discoveryClient).isInstanceOf(MyScopedDiscoveryClient.class);

		List<String> hosts = Arrays.asList("h.host", "i.host", "h.host");

		for (String host : hosts) {
			ServiceInstance instance = this.loadBalancer.choose("thirdservice");
			assertThat(instance).isNotNull();
			assertThat(instance.getHost()).isEqualTo(host).as("instance host is incorrent %s", host);
		}
	}

	@EnableAutoConfiguration
	@SpringBootConfiguration
	@LoadBalancerClients ({ //TODO: move to auto-configuration
			@LoadBalancerClient(name = "anotherservice", configuration = AnotherServiceConfig.class),
			@LoadBalancerClient(name = "thirdservice", configuration = ThirdServiceConfig.class),
	})
	protected static class Config {

		@Autowired(required = false)
		private List<LoadBalancerClientSpecification> configurations = new ArrayList<>();

		//TODO: move to auto-configuration
		@Bean
		public LoadBalancerClientFactory loadBalancerClientFactory() {
			LoadBalancerClientFactory clientFactory = new LoadBalancerClientFactory();
			clientFactory.setConfigurations(configurations);
			return clientFactory;
		}

		//TODO: move to auto-configuration
		@Bean
		public CommonsLoadBalancer commonsLoadBalancer(LoadBalancerClientFactory clientFactory) {
			return new CommonsLoadBalancer(clientFactory);
		}

	}

	protected static class AnotherServiceConfig {
		@Bean
		public ScopedLoadBalancer randomAlgorithm(LoadBalancerClientFactory clientFactory) {
			return new RandomScopedLoadBalancer(clientFactory);
		}
	}

	protected static class ThirdServiceConfig {
		@Bean
		@Primary
		public ScopedDiscoveryClient filteredDiscoveryClient(CachedDiscoveryClient delegate) {
			return new MyScopedDiscoveryClient(delegate);
		}
	}

	private static class MyScopedDiscoveryClient implements ScopedDiscoveryClient {
		private final ScopedDiscoveryClient delegate;

		public MyScopedDiscoveryClient(ScopedDiscoveryClient delegate) {
			this.delegate = delegate;
		}

		@Override
		public List<ServiceInstance> getInstances() {
			List<ServiceInstance> instances = delegate.getInstances();
			return instances.stream()
					.filter(instance -> !instance.getHost().startsWith("g"))
					.collect(Collectors.toList());
		}

	}
}
