package org.springframework.cloud.client.loadbalancer.impl.scoped;

import java.util.List;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.impl.support.LoadBalancerClientFactory;

import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * @author Spencer Gibb
 */
public class RandomScopedLoadBalancer implements ScopedLoadBalancer {

	private static final Log log = LogFactory.getLog(RandomScopedLoadBalancer.class);

	private final LoadBalancerClientFactory clientFactory;
	private final Random random = new Random();

	@Value("${loadbalancer.client.name}")
	String serviceId;

	public RandomScopedLoadBalancer(LoadBalancerClientFactory clientFactory) {
		this.clientFactory = clientFactory;
	}

	@Override
	public ServiceInstance choose() {
		ScopedDiscoveryClient discoveryClient = this.clientFactory.getInstance(this.serviceId, ScopedDiscoveryClient.class);
		List<ServiceInstance> instances = discoveryClient.getInstances();

		if (isEmpty(instances)) {
			log.warn("No servers available for service: " + serviceId);
			return null;
		}

		int nextServerIndex = random.nextInt(instances.size());

		ServiceInstance instance = instances.get(nextServerIndex);

		return instance;
	}
}
