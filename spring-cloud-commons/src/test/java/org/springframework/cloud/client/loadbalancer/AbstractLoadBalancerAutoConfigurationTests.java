/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.client.loadbalancer;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer.REQUEST;

/**
 * @author Ryan Baxter
 * @author Tim Ysewyn
 * @author Olga Maciaszek-Sharma
 */
public abstract class AbstractLoadBalancerAutoConfigurationTests {

	@Test
	public void restTemplateGetsLoadBalancerInterceptor() {
		ConfigurableApplicationContext context = init(OneRestTemplate.class);
		final Map<String, RestTemplate> restTemplates = context.getBeansOfType(RestTemplate.class);

		then(restTemplates).isNotNull();
		then(restTemplates.values()).hasSize(1);
		RestTemplate restTemplate = restTemplates.values().iterator().next();
		then(restTemplate).isNotNull();

		assertLoadBalanced(restTemplate);
	}

	protected abstract void assertLoadBalanced(RestTemplate restTemplate);

	@Test
	public void multipleRestTemplates() {
		ConfigurableApplicationContext context = init(TwoRestTemplates.class);
		final Map<String, RestTemplate> restTemplates = context.getBeansOfType(RestTemplate.class);

		then(restTemplates).isNotNull();
		Collection<RestTemplate> templates = restTemplates.values();
		then(templates).hasSize(2);

		TwoRestTemplates.Two two = context.getBean(TwoRestTemplates.Two.class);

		then(two.loadBalanced).isNotNull();
		assertLoadBalanced(two.loadBalanced);

		then(two.nonLoadBalanced).isNotNull();
		then(two.nonLoadBalanced.getInterceptors()).isEmpty();
	}

	protected ConfigurableApplicationContext init(Class<?> config) {
		return init(config, "spring.aop.proxyTargetClass=true");
	}

	protected ConfigurableApplicationContext init(Class<?> config, String... props) {
		return new SpringApplicationBuilder().web(WebApplicationType.NONE).properties(props)
				.sources(config, LoadBalancerAutoConfiguration.class).run();
	}

	@Configuration(proxyBeanMethods = false)
	protected static class OneRestTemplate {

		@LoadBalanced
		@Bean
		RestTemplate loadBalancedRestTemplate() {
			return new RestTemplate();
		}

		@Bean
		LoadBalancerClient loadBalancerClient() {
			return new NoopLoadBalancerClient();
		}

		@Bean
		ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory(LoadBalancerProperties properties) {
			return new TestLoadBalancerFactory(properties);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(OneRestTemplate.class)
	protected static class TwoRestTemplates {

		@Primary
		@Bean
		RestTemplate restTemplate() {
			return new RestTemplate();
		}

		@Configuration(proxyBeanMethods = false)
		protected static class Two {

			@Autowired
			RestTemplate nonLoadBalanced;

			@Autowired
			@LoadBalanced
			RestTemplate loadBalanced;

		}

	}

	private static class NoopLoadBalancerClient implements LoadBalancerClient {

		private final Random random = new Random();

		@Override
		public ServiceInstance choose(String serviceId) {
			return choose(serviceId, REQUEST);
		}

		@Override
		public <T> ServiceInstance choose(String serviceId, Request<T> request) {
			return new DefaultServiceInstance(serviceId, serviceId, serviceId, this.random.nextInt(40000), false);
		}

		@Override
		public <T> T execute(String serviceId, LoadBalancerRequest<T> request) {
			try {
				return request.apply(choose(serviceId, REQUEST));
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public <T> T execute(String serviceId, ServiceInstance serviceInstance, LoadBalancerRequest<T> request) {
			try {
				return request.apply(choose(serviceId, REQUEST));
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public URI reconstructURI(ServiceInstance instance, URI original) {
			return DefaultServiceInstance.getUri(instance);
		}

	}

	private static class TestLoadBalancerFactory implements ReactiveLoadBalancer.Factory<ServiceInstance> {

		private final LoadBalancerProperties properties;

		TestLoadBalancerFactory(LoadBalancerProperties properties) {
			this.properties = properties;
		}

		@Override
		public ReactiveLoadBalancer<ServiceInstance> getInstance(String serviceId) {
			throw new UnsupportedOperationException("Not implemented.");
		}

		@Override
		public Object getInstance(String name, Class clazz, Class[] generics) {
			throw new UnsupportedOperationException("Not implemented.");
		}

		@Override
		public Map getInstances(String name, Class type) {
			throw new UnsupportedOperationException("Not implemented.");
		}

		@Override
		public LoadBalancerProperties getProperties(String serviceId) {
			return properties;
		}

	}

}
