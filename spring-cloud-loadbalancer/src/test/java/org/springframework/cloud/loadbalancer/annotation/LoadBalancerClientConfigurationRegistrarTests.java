package org.springframework.cloud.loadbalancer.annotation;


import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycle;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * @author Olga Maciaszek-Sharma
 */
class LoadBalancerClientConfigurationRegistrarTests {


	@SpringBootTest
	protected static class ContextWithDefaultsTests {

		private static final String SERVICE_A_LIFECYCLE_KEY = "loadBalancerClientConfigurationRegistrarTests.ContextWithDefaultsTests.ServiceALifecycle";
		private static final String DEFAULT_LIFECYCLE_KEY = "loadBalancerClientConfigurationRegistrarTests.ContextWithDefaultsTests.DefaultLifecycle";
		@Autowired
		ApplicationContext context;

		@Autowired
		private LoadBalancerClientFactory clientFactory;

		@Test
		void shouldRegisterServiceAndDefaultLifecycleProcessor() {
			Map<String, LoadBalancerLifecycle> processors = clientFactory
					.getInstances("serviceA", LoadBalancerLifecycle.class);

			assertThat(processors).hasSize(2);
			assertThat(processors)
					.extractingByKey(SERVICE_A_LIFECYCLE_KEY)
					.isInstanceOf(ServiceALifecycle.class);
			assertThat(processors)
					.extractingByKey(DEFAULT_LIFECYCLE_KEY)
					.isInstanceOf(DefaultLifecycle.class);
		}

		@Test
		void shouldRegisterOnlyDefaultLifecycleProcessor() {
			Map<String, LoadBalancerLifecycle> processors = clientFactory
					.getInstances("serviceB", LoadBalancerLifecycle.class);

			assertThat(processors).hasSize(1);
			assertThat(processors)
					.extractingByKey(DEFAULT_LIFECYCLE_KEY)
					.isInstanceOf(DefaultLifecycle.class);
		}

		@Configuration(proxyBeanMethods = false)
		@EnableAutoConfiguration
		@LoadBalancerClients(value = {@LoadBalancerClient(name = "serviceA",
				lifecycleProcessors = ServiceALifecycle.class),
				@LoadBalancerClient(name = "serviceB",
						configuration = ServiceConfig.class)},
				defaultLifecycleProcessors = DefaultLifecycle.class)
		protected static class Config {

		}

		protected static class ServiceALifecycle implements LoadBalancerLifecycle {

			@Override
			public <RC> void onStart(Request<RC> request) {

			}

			@Override
			public <RES, T> void onComplete(CompletionContext<RES, T> completionContext) {

			}
		}

		protected static class DefaultLifecycle implements LoadBalancerLifecycle {

			@Override
			public <RC> void onStart(Request<RC> request) {

			}

			@Override
			public <RES, T> void onComplete(CompletionContext<RES, T> completionContext) {

			}
		}
	}

	@SpringBootTest
	protected static class ContextWithNoDefaultsTests {

		@Autowired
		LoadBalancerClientFactory clientFactory;

		@Test
		void shouldWorkWhenNoLifecycleProcessorDefined() {
			assertThatCode(() -> {
				Map<String, LoadBalancerLifecycle> processors = clientFactory
						.getInstances("serviceC", LoadBalancerLifecycle.class);
				assertThat(processors).isNull();
			}).doesNotThrowAnyException();
		}


		@Configuration(proxyBeanMethods = false)
		@EnableAutoConfiguration
		@LoadBalancerClient(name = "serviceC", configuration = ServiceConfig.class)
		protected static class NoLifecycleProcessors {

		}
	}

	public static class ServiceConfig {
		@Bean
		public RoundRobinLoadBalancer roundRobinContextLoadBalancer(
				LoadBalancerClientFactory clientFactory, Environment env) {
			String serviceId = clientFactory.getName(env);
			return new RoundRobinLoadBalancer(clientFactory.getLazyProvider(serviceId,
					ServiceInstanceListSupplier.class), serviceId, -1);
		}
	}

}