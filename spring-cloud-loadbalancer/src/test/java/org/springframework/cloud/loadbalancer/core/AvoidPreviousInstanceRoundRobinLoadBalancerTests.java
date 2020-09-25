package org.springframework.cloud.loadbalancer.core;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.client.loadbalancer.RetryableRequestContext;
import org.springframework.cloud.loadbalancer.support.ServiceInstanceListSuppliers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olga Maciaszek-Sharma
 */
class AvoidPreviousInstanceRoundRobinLoadBalancerTests {

	private final String serviceId = "test";

	private static DefaultServiceInstance instance(String serviceId, String host, boolean secure) {
		return new DefaultServiceInstance(serviceId, serviceId, host, 80, secure);
	}

	@Test
	void shouldReturnEmptyResponseIfNoInstances() {
		AvoidPreviousInstanceRoundRobinLoadBalancer loadBalancer = new AvoidPreviousInstanceRoundRobinLoadBalancer(
				ServiceInstanceListSuppliers.toProvider(serviceId), serviceId);

		Response<ServiceInstance> response = loadBalancer
				.choose(new DefaultRequest<>(new RetryableRequestContext(null)))
				.block();

		assertThat(response).isInstanceOf(EmptyResponse.class);
	}

	@Test
	void shouldReturnADifferentInstance() {
		ServiceInstance firstInstance = instance(serviceId, "1host", false);
		ServiceInstance secondInstance = instance(serviceId, "2host-secure", true);
		AvoidPreviousInstanceRoundRobinLoadBalancer loadBalancer = new AvoidPreviousInstanceRoundRobinLoadBalancer(
				ServiceInstanceListSuppliers
						.toProvider(serviceId, firstInstance, secondInstance), serviceId);

		Response<ServiceInstance> firstResponse = loadBalancer
				.choose(new DefaultRequest<>(new RetryableRequestContext(firstInstance)))
				.block();
		Response<ServiceInstance> secondResponse = loadBalancer
				.choose(new DefaultRequest<>(new RetryableRequestContext(firstInstance)))
				.block();

		assertThat(firstResponse.getServer()).isEqualTo(secondInstance);
		assertThat(secondResponse.getServer()).isEqualTo(secondInstance);
	}

	@Test
	void shouldReturnSameInstanceIfDifferentOneNotAvailable() {
		ServiceInstance firstInstance = instance(serviceId, "1host", false);
		AvoidPreviousInstanceRoundRobinLoadBalancer loadBalancer = new AvoidPreviousInstanceRoundRobinLoadBalancer(
				ServiceInstanceListSuppliers
						.toProvider(serviceId, firstInstance), serviceId);

		Response<ServiceInstance> firstResponse = loadBalancer
				.choose(new DefaultRequest<>(new RetryableRequestContext(firstInstance)))
				.block();

		assertThat(firstResponse.getServer()).isEqualTo(firstInstance);
	}

}