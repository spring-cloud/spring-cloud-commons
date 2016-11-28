package org.springframework.cloud.client.loadbalancer;

import lombok.SneakyThrows;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.AsyncClientHttpRequestInterceptor;
import org.springframework.web.client.AsyncRestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.empty;


/**
 * @author Rob Worsnop
 */
public class AsyncLoadBalancerAutoConfigurationTests {

	@Test
	public void restTemplateGetsLoadBalancerInterceptor() {
		ConfigurableApplicationContext context = init(OneRestTemplate.class);
		final Map<String, AsyncRestTemplate> restTemplates = context
				.getBeansOfType(AsyncRestTemplate.class);

		MatcherAssert.assertThat(restTemplates, is(notNullValue()));
		MatcherAssert.assertThat(restTemplates.values(), hasSize(1));
		AsyncRestTemplate restTemplate = restTemplates.values().iterator().next();
		MatcherAssert.assertThat(restTemplate, is(notNullValue()));

		assertLoadBalanced(restTemplate);
	}

	private void assertLoadBalanced(AsyncRestTemplate restTemplate) {
		List<AsyncClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
		MatcherAssert.assertThat(interceptors, hasSize(1));
		AsyncClientHttpRequestInterceptor interceptor = interceptors.get(0);
		MatcherAssert.assertThat(interceptor, is(instanceOf(AsyncLoadBalancerInterceptor.class)));
	}

	@Test
	public void multipleRestTemplates() {
		ConfigurableApplicationContext context = init(TwoRestTemplates.class);
		final Map<String, AsyncRestTemplate> restTemplates = context
				.getBeansOfType(AsyncRestTemplate.class);

		MatcherAssert.assertThat(restTemplates, is(notNullValue()));
		Collection<AsyncRestTemplate> templates = restTemplates.values();
		MatcherAssert.assertThat(templates, hasSize(2));

		TwoRestTemplates.Two two = context.getBean(TwoRestTemplates.Two.class);

		MatcherAssert.assertThat(two.loadBalanced, is(notNullValue()));
		assertLoadBalanced(two.loadBalanced);

		MatcherAssert.assertThat(two.nonLoadBalanced, is(notNullValue()));
		MatcherAssert.assertThat(two.nonLoadBalanced.getInterceptors(), is(empty()));
	}

	protected ConfigurableApplicationContext init(Class<?> config) {
		return new SpringApplicationBuilder().web(false)
				.properties("spring.aop.proxyTargetClass=true")
				.sources(config, AsyncLoadBalancerAutoConfiguration.class).run();
	}

	@Configuration
	protected static class OneRestTemplate {

		@LoadBalanced
		@Bean
		AsyncRestTemplate loadBalancedRestTemplate() {
			return new AsyncRestTemplate();
		}

		@Bean
		LoadBalancerClient loadBalancerClient() {
			return new NoopLoadBalancerClient();
		}

		@Bean
		LoadBalancedRetryPolicyFactory loadBalancedRetryPolicyFactory() { return new LoadBalancedRetryPolicyFactory.NeverRetryFactory();}

	}

	@Configuration
	protected static class TwoRestTemplates {

		@Primary
		@Bean
		AsyncRestTemplate restTemplate() {
			return new AsyncRestTemplate();
		}

		@LoadBalanced
		@Bean
		AsyncRestTemplate loadBalancedRestTemplate() {
			return new AsyncRestTemplate();
		}

		@Bean
		LoadBalancerClient loadBalancerClient() {
			return new NoopLoadBalancerClient();
		}

		@Configuration
		protected static class Two {
			@Autowired
			AsyncRestTemplate nonLoadBalanced;

			@Autowired
			@LoadBalanced
			AsyncRestTemplate loadBalanced;
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
		@SneakyThrows
		public <T> T execute(String serviceId, ServiceInstance serviceInstance, LoadBalancerRequest<T> request) throws IOException {
			return request.apply(choose(serviceId));
		}

		@Override
		public URI reconstructURI(ServiceInstance instance, URI original) {
			return DefaultServiceInstance.getUri(instance);
		}
	}
}
