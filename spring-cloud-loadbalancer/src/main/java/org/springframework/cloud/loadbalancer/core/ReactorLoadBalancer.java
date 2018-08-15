package org.springframework.cloud.loadbalancer.core;

import reactor.core.publisher.Mono;

import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.client.loadbalancer.reactive.Request;
import org.springframework.cloud.client.loadbalancer.reactive.Response;

public interface ReactorLoadBalancer<T> extends ReactiveLoadBalancer<T> {
	/**
	 * Choose the next server based on the load balancing algorithm
	 * @param request
	 * @return
	 */
	Mono<Response<T>> choose(Request request);

	default Mono<Response<T>> choose() {
		return choose(REQUEST);
	}
}
