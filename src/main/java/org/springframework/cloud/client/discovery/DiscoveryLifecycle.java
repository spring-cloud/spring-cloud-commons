package org.springframework.cloud.client.discovery;

import org.springframework.context.SmartLifecycle;
import org.springframework.core.Ordered;

/**
 * @author Spencer Gibb
 */
public interface DiscoveryLifecycle extends SmartLifecycle, Ordered {
}
