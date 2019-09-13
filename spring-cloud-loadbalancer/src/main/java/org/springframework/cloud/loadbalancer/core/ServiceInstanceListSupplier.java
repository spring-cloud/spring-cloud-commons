package org.springframework.cloud.loadbalancer.core;

import java.util.List;
import java.util.function.Supplier;

import reactor.core.publisher.Mono;

import org.springframework.cloud.client.ServiceInstance;

/**
 * @author Olga Maciaszek-Sharma
 */
public interface ServiceInstanceListSupplier<T extends ServiceInstance> extends Supplier<Mono<List<T>>> {

	String getServiceId();
}
