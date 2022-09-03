package org.springframework.cloud.loadbalancer.core;

import org.springframework.cloud.client.ServiceInstance;

@FunctionalInterface
public interface WeightFunction {

	int apply(ServiceInstance instance);
}
