package org.springframework.cloud.loadbalancer.filter;

import org.springframework.cloud.client.ServiceInstance;

public interface ServerInstanceFilter<T> {
    boolean filter(ServiceInstance serviceInstance);
}
