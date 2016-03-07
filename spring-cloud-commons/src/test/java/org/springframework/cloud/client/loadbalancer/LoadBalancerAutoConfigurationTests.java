package org.springframework.cloud.client.loadbalancer;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import lombok.SneakyThrows;

/**
 * @author Spencer Gibb
 */
public class LoadBalancerAutoConfigurationTests {

	@Test
	public void restTemplateGetsLoadBalancerInterceptor() {
		ConfigurableApplicationContext context = init(OneRestTemplate.class);
		final Map<String, RestTemplate> restTemplates = context
				.getBeansOfType(RestTemplate.class);

		assertThat(restTemplates, is(notNullValue()));
		assertThat(restTemplates.values(), hasSize(1));
		RestTemplate restTemplate = restTemplates.values().iterator().next();
		assertThat(restTemplate, is(notNullValue()));

		assertLoadBalanced(restTemplate);
	}

	protected void assertLoadBalanced(RestTemplate restTemplate) {
		List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
		assertThat(interceptors, hasSize(1));
		ClientHttpRequestInterceptor interceptor = interceptors.get(0);
		assertThat(interceptor, is(instanceOf(LoadBalancerInterceptor.class)));
	}

	@Test
	public void multipleRestTemplates() {
		ConfigurableApplicationContext context = init(TwoRestTemplates.class);
		final Map<String, RestTemplate> restTemplates = context
				.getBeansOfType(RestTemplate.class);

		assertThat(restTemplates, is(notNullValue()));
		Collection<RestTemplate> templates = restTemplates.values();
		assertThat(templates, hasSize(2));

		TwoRestTemplates.Two two = context.getBean(TwoRestTemplates.Two.class);

		assertThat(two.loadBalanced, is(notNullValue()));
		assertLoadBalanced(two.loadBalanced);

		assertThat(two.nonLoadBalanced, is(notNullValue()));
		assertThat(two.nonLoadBalanced.getInterceptors(), is(empty()));
	}

	protected ConfigurableApplicationContext init(Class<?> config) {
		return new SpringApplicationBuilder().web(false)
				.properties("spring.aop.proxyTargetClass=true")
				.sources(config, LoadBalancerAutoConfiguration.class).run();
	}

	@Configuration
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

	}

	@Configuration
	protected static class TwoRestTemplates {

		@Primary
		@Bean
		RestTemplate restTemplate() {
			return new RestTemplate();
		}

		@LoadBalanced
		@Bean
		RestTemplate loadBalancedRestTemplate() {
			return new RestTemplate();
		}

		@Bean
		LoadBalancerClient loadBalancerClient() {
			return new NoopLoadBalancerClient();
		}

		@Configuration
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
			return new DefaultServiceInstance(serviceId, serviceId,
					this.random.nextInt(40000), false);
		}

		@Override
		@SneakyThrows
		public <T> T execute(String serviceId, LoadBalancerRequest<T> request) {
			return request.apply(choose(serviceId));
		}

		@Override
		public URI reconstructURI(ServiceInstance instance, URI original) {
			return DefaultServiceInstance.getUri(instance);
		}
	}
}
