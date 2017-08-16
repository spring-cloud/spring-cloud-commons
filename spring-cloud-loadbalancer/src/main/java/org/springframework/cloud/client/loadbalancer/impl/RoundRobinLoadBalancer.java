package org.springframework.cloud.client.loadbalancer.impl;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.impl.support.LoadBalancerClientFactory;
import org.springframework.core.env.Environment;

import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * @author Spencer Gibb
 */
public class RoundRobinLoadBalancer implements LoadBalancer<ServiceInstance> {

	private static final Log log = LogFactory.getLog(RoundRobinLoadBalancer.class);

	private final AtomicInteger nextServerCyclicCounter = new AtomicInteger(-1);
	private final LoadBalancerClientFactory clientFactory;
	private final Environment environment;

	public RoundRobinLoadBalancer(LoadBalancerClientFactory clientFactory, Environment environment) {
		this.clientFactory = clientFactory;
		this.environment = environment;
	}

	@Override
	public Response<ServiceInstance> choose(Request request) {
		String serviceId = clientFactory.getName(this.environment);
		ServiceInstanceSupplier supplier = clientFactory.getInstance(serviceId, ServiceInstanceSupplier.class);
		List<ServiceInstance> instances = supplier.get();
		//TODO: enforce order?

		if (isEmpty(instances)) {
			log.warn("No servers available for service: " + serviceId);
			return new DefaultResponse(null);
		}

		int nextServerIndex = incrementAndGetModulo(instances.size());

		ServiceInstance instance = instances.get(nextServerIndex);

		return new DefaultResponse(instance);
	}

	/**
	 * Inspired by the implementation of {@link AtomicInteger#incrementAndGet()}.
	 * <p>
	 * original https://github.com/Netflix/ribbon/blob/master/ribbon-loadbalancer/src/main/java/com/netflix/loadbalancer/RoundRobinRule.java#L94-L107
	 *
	 * @param modulo The modulo to bound the value of the counter.
	 * @return The next value.
	 */
	private int incrementAndGetModulo(int modulo) {
		for (; ; ) {
			int current = nextServerCyclicCounter.get();
			int next = (current + 1) % modulo;
			if (nextServerCyclicCounter.compareAndSet(current, next))
				return next;
		}

	}
}
