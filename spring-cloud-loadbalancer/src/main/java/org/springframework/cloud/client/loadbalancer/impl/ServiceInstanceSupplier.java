package org.springframework.cloud.client.loadbalancer.impl;

import org.springframework.cloud.client.ServiceInstance;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author Spencer Gibb
 */
public interface ServiceInstanceSupplier extends Supplier<List<ServiceInstance>> {
}
