package org.springframework.cloud.client.loadbalancer;

import java.net.URI;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.support.HttpRequestWrapper;

/**
 * @author Ryan Baxter
 */
public class ServiceRequestWrapper extends HttpRequestWrapper {
	private final ServiceInstance instance;
	private final LoadBalancerClient loadBalancer;

	public ServiceRequestWrapper(HttpRequest request, ServiceInstance instance,
								 LoadBalancerClient loadBalancer) {
		super(request);
		this.instance = instance;
		this.loadBalancer = loadBalancer;
	}

	@Override
	public URI getURI() {
		URI uri = this.loadBalancer.reconstructURI(
				this.instance, getRequest().getURI());
		return uri;
	}
}
