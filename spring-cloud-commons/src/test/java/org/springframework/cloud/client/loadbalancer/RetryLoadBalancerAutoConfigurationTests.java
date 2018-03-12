package org.springframework.cloud.client.loadbalancer;

import java.util.List;

import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * @author Ryan Baxter
 */
public class RetryLoadBalancerAutoConfigurationTests extends AbstractLoadBalancerAutoConfigurationTests {
	@Override
	protected void assertLoadBalanced(RestTemplate restTemplate) {
		List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
		assertThat(interceptors, hasSize(1));
		ClientHttpRequestInterceptor interceptor = interceptors.get(0);
		assertThat(interceptor, is(instanceOf(RetryLoadBalancerInterceptor.class)));
	}

	@Test
	public void testDefaultBackOffPolicy() throws Exception {
		ConfigurableApplicationContext context = init(OneRestTemplate.class);
		LoadBalancedRetryFactory loadBalancedRetryFactory = context.getBean(LoadBalancedRetryFactory.class);
		assertThat(loadBalancedRetryFactory, is(instanceOf(LoadBalancedRetryFactory.class)));
		assertThat(loadBalancedRetryFactory.createBackOffPolicy("foo"), is(instanceOf(NoBackOffPolicy.class)));
	}
}

