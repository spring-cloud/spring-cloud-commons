package org.springframework.cloud.client.loadbalancer.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.impl.OnComplete.Status;
import org.springframework.cloud.client.loadbalancer.impl.annotation.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.impl.annotation.LoadBalancerClientSpecification;
import org.springframework.cloud.client.loadbalancer.impl.annotation.LoadBalancerClients;
import org.springframework.cloud.client.loadbalancer.impl.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Spencer Gibb
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class LoadBalancerTest {

	@Autowired
	private LoadBalancerClientFactory clientFactory;

	@Test
	public void roundRobbinLoadbalancerWorks() {
		ResolvableType type = ResolvableType.forClassWithGenerics(LoadBalancer.class, ServiceInstance.class);
		LoadBalancer<ServiceInstance> loadBalancer = this.clientFactory.getInstance("myservice", type);

		assertThat(loadBalancer).isInstanceOf(RoundRobinLoadBalancer.class);

		List<String> hosts = Arrays.asList("a.host", "c.host", "b.host", "a.host");

		for (String host : hosts) {
			LoadBalancer.Response<ServiceInstance> response = loadBalancer.choose(new DefaultRequest());

			assertThat(response.hasServer()).isTrue();

			ServiceInstance instance = response.getServer();
			assertThat(instance).isNotNull();
			assertThat(instance.getHost()).isEqualTo(host).as("instance host is incorrent %s", host);

			if (host.equals("b.host")) {
				assertThat(instance.isSecure()).isTrue();
			} else {
				assertThat(instance.isSecure()).isFalse();
			}

			response.onComplete(new OnComplete(Status.SUCCESSS));
		}
	}

	@EnableAutoConfiguration
	@SpringBootConfiguration
	@LoadBalancerClients ({ //TODO: move to auto-configuration
			@LoadBalancerClient(name = "myservice", configuration = MyServiceConfig.class),
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

	}

	protected static class MyServiceConfig {
		@Bean
		public RoundRobinLoadBalancer roundRobinContextLoadBalancer(LoadBalancerClientFactory clientFactory, Environment env) {
			return new RoundRobinLoadBalancer(clientFactory, env);
		}
	}
}
