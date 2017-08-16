/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.loadbalancer.core;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.core.OnComplete.Status;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
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
	@LoadBalancerClients({
			@LoadBalancerClient(name = "myservice", configuration = MyServiceConfig.class),
	})
	protected static class Config { }

	protected static class MyServiceConfig {
		@Bean
		public RoundRobinLoadBalancer roundRobinContextLoadBalancer(LoadBalancerClientFactory clientFactory, Environment env) {
			return new RoundRobinLoadBalancer(clientFactory, env);
		}
	}
}
