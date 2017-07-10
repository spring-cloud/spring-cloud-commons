package org.springframework.cloud.client.loadbalancer.impl.context;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.impl.scoped.ScopedDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.impl.support.LoadBalancerClientFactory;

import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * @author Spencer Gibb
 */
public class RoundRobinLoadBalancer implements LoadBalancer<ServiceInstance> {

	private static final Log log = LogFactory.getLog(RoundRobinLoadBalancer.class);

	private final LoadBalancerClientFactory clientFactory;
	private final AtomicInteger nextServerCyclicCounter = new AtomicInteger(-1);

	@Value("${loadbalancer.client.name}")
	String serviceId;

	public RoundRobinLoadBalancer(LoadBalancerClientFactory clientFactory) {
		this.clientFactory = clientFactory;
	}

	@Override
	public Context<ServiceInstance> choose() {
		ScopedDiscoveryClient discoveryClient = this.clientFactory.getInstance(this.serviceId, ScopedDiscoveryClient.class);
		List<ServiceInstance> instances = discoveryClient.getInstances();

		if (isEmpty(instances)) {
			log.warn("No servers available for service: " + this.serviceId);
			return new DefaultContext(null);
		}

		int nextServerIndex = incrementAndGetModulo(instances.size());

		ServiceInstance instance = instances.get(nextServerIndex);

		return new DefaultContext(instance);
	}

	public static class DefaultContext implements Context<ServiceInstance> {

		private final ServiceInstance serviceInstance;

		public DefaultContext(ServiceInstance serviceInstance) {
			this.serviceInstance = serviceInstance;
		}

		@Override
		public boolean hasServer() {
			return serviceInstance != null;
		}

		@Override
		public ServiceInstance getServer() {
			return this.serviceInstance;
		}

		@Override
		public void complete(Status status) {
			//TODO: implement
		}
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
