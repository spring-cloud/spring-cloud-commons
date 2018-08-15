package org.springframework.cloud.client.loadbalancer.reactive;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public interface CacheSupplier extends Supplier<Map<String, List>> {
}
