package org.springframework.cloud.client.serviceregistry;

import org.springframework.cloud.client.ServiceInstance;

/**
 * A marker interface used by a {@link ServiceRegistry}.
 *
 * @author Spencer Gibb
 * @since 1.2.0
 */
public interface Registration extends ServiceInstance {
}
