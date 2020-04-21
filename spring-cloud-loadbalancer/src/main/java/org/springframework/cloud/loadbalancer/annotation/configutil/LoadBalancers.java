package org.springframework.cloud.loadbalancer.annotation.configutil;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.core.env.Environment;

/**
 * Utility class containing helper methods for setting up {@link ReactorLoadBalancer<ServiceInstance>} configuration
 * in a more concise way.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.3
 */
public class LoadBalancers {

	private LoadBalancers() {
		throw new IllegalStateException("Can't instantiate a utility class");
	}

	public static ReactorLoadBalancer<ServiceInstance> roundRobin(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
			String serviceId) {
		return new RoundRobinLoadBalancer(serviceInstanceListSupplierProvider, serviceId);
	}

	public static ReactorLoadBalancer<ServiceInstance> roundRobin(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
			Environment environment) {
		return new RoundRobinLoadBalancer(serviceInstanceListSupplierProvider,
				environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME));
	}

	public static ReactorLoadBalancer<ServiceInstance> roundRobin(LoadBalancerClientFactory loadBalancerClientFactory,
			String serviceId) {
		return roundRobin(serviceInstanceListSupplierProvider(loadBalancerClientFactory, serviceId), serviceId);
	}

	public static ReactorLoadBalancer<ServiceInstance> roundRobin(LoadBalancerClientFactory loadBalancerClientFactory,
			Environment environment) {
		String serviceId = environment
				.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
		return new RoundRobinLoadBalancer(serviceInstanceListSupplierProvider(loadBalancerClientFactory, serviceId),
				serviceId);
	}

	private static ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider(LoadBalancerClientFactory loadBalancerClientFactory, String serviceId) {
		return loadBalancerClientFactory
				.getLazyProvider(serviceId, ServiceInstanceListSupplier.class);
	}

}
