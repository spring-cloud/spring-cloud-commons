package org.springframework.cloud.client.loadbalancer.reactive;

import java.net.URI;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import reactor.core.publisher.Mono;

/**
 * @author Spencer Gibb
 */
public class LoadBalancerExchangeFilterFunction implements ExchangeFilterFunction {

	private final LoadBalancerClient loadBalancerClient;

	public LoadBalancerExchangeFilterFunction(LoadBalancerClient loadBalancerClient) {
		this.loadBalancerClient = loadBalancerClient;
	}

	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
		URI originalUrl = request.url();
		String serviceId = originalUrl.getHost();
		Assert.state(serviceId != null, "Request URI does not contain a valid hostname: " + originalUrl);
		//TODO: reactive lb client

		ServiceInstance instance = this.loadBalancerClient.choose(serviceId);
		URI uri = this.loadBalancerClient.reconstructURI(instance, originalUrl);
		ClientRequest newRequest = ClientRequest.method(request.method(), uri)
				.headers(headers -> headers.addAll(request.headers()))
				.cookies(cookies -> cookies.addAll(request.cookies()))
				.attributes(attributes -> attributes.putAll(request.attributes()))
				.body(request.body())
				.build();
		return next.exchange(newRequest);
	}

}
