package org.springframework.cloud.loadbalancer.core;

import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.util.UriComponentsBuilder;

import reactor.core.publisher.Mono;

/**
 * @author Spencer Gibb
 * @author Ryan Baxter
 */
public class ReactiveLoadBalancerExchangeFilterFunction
		implements ExchangeFilterFunction {

	private static Log logger = LogFactory
			.getLog(ReactiveLoadBalancerExchangeFilterFunction.class);

	private final LoadBalancerClientFactory loadBalancerClientFactory;

	public ReactiveLoadBalancerExchangeFilterFunction(
			LoadBalancerClientFactory loadBalancerClientFactory) {
		this.loadBalancerClientFactory = loadBalancerClientFactory;
	}

	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
		URI originalUrl = request.url();
		String serviceId = originalUrl.getHost();
		if (serviceId == null) {
			String msg = String.format(
					"Request URI does not contain a valid hostname: %s",
					originalUrl.toString());
			logger.warn(msg);
			return Mono.just(
					ClientResponse.create(HttpStatus.BAD_REQUEST).body(msg).build());
		}

		ReactiveLoadBalancer<ServiceInstance> reactiveLoadBalancer = loadBalancerClientFactory
				.getInstance(serviceId, ReactiveLoadBalancer.class,
						ServiceInstance.class);

		if (reactiveLoadBalancer == null) {
			String msg = String.format("Load balancer can not find service %s",
					serviceId);
			logger.warn(msg);
			return Mono.just(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE)
					.body(msg).build());
		}

		return Mono.from(reactiveLoadBalancer.choose()).flatMap(si -> {
			if (si.hasServer()) {
				return next.exchange(ClientRequest.from(request)
						.url(reconstructURI(si.getServer(), originalUrl)).build());
			}
			String msg = String.format(
					"Load balancer does not contain an instance for the service %s",
					serviceId);
			logger.warn(msg);
			return Mono.just(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE)
					.body(msg).build());
		});
	}

	protected URI reconstructURI(ServiceInstance instance, URI original) {
		return UriComponentsBuilder.fromUri(original)
				.scheme(instance.getUri().getScheme()).host(instance.getHost())
				.port(instance.getPort()).build().toUri();
	}
}
