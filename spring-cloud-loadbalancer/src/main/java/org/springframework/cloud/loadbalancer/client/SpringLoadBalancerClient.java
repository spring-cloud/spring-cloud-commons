package org.springframework.cloud.loadbalancer.client;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import reactor.core.publisher.Mono;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools;
import org.springframework.cloud.client.loadbalancer.reactive.Response;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.util.ReflectionUtils;

/**
 * @author Olga Maciaszek-Sharma
 */
public class SpringLoadBalancerClient implements LoadBalancerClient {

	private final LoadBalancerClientFactory loadBalancerClientFactory;

	public SpringLoadBalancerClient(LoadBalancerClientFactory loadBalancerClientFactory) {
		this.loadBalancerClientFactory = loadBalancerClientFactory;
	}

	@Override
	public <T> T execute(String serviceId, LoadBalancerRequest<T> request) throws IOException {
		ServiceInstance serviceInstance = Optional.ofNullable(choose(serviceId))
				.orElseThrow(() -> new IllegalStateException("No instances available for " + serviceId));
		return execute(serviceId, serviceInstance, request);
	}

	@Override
	public <T> T execute(String serviceId, ServiceInstance serviceInstance, LoadBalancerRequest<T> request) throws IOException {
		try {
			return request.apply(serviceInstance);
		}
		catch (IOException iOException) {
			throw iOException;
		}
		catch (Exception exception) {
			ReflectionUtils.rethrowRuntimeException(exception);
		}
		return null;
	}


	@Override
	public URI reconstructURI(ServiceInstance serviceInstance, URI original) {
		return LoadBalancerUriTools.reconstructURI(serviceInstance, original);
	}

	@Override
	public ServiceInstance choose(String serviceId) {
		return Optional.ofNullable(loadBalancerClientFactory.getInstance(serviceId))
				.map(loadBalancer -> Mono.from(loadBalancer.choose())
						.map(Response::getServer)
						.block()).orElse(null);
	}
}
