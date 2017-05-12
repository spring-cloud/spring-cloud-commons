package org.springframework.cloud.client.loadbalancer;

import java.util.List;

import org.junit.runner.RunWith;

import org.springframework.cloud.test.ClassPathExclusions;
import org.springframework.cloud.test.ModifiedClassPathRunner;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

/**
 * @author Spencer Gibb
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions({"spring-retry-*.jar", "spring-boot-starter-aop-*.jar"})
public class LoadBalancerAutoConfigurationTests extends AbstractLoadBalancerAutoConfigurationTests {

	@Override
	protected void assertLoadBalanced(RestTemplate restTemplate) {
		List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
		assertThat(interceptors, hasSize(1));
		ClientHttpRequestInterceptor interceptor = interceptors.get(0);
		assertThat(interceptor, is(instanceOf(LoadBalancerInterceptor.class)));
	}
}
