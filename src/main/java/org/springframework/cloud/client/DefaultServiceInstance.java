package org.springframework.cloud.client;

import lombok.Data;

/**
 * @author Spencer Gibb
 */
@Data
public class DefaultServiceInstance implements ServiceInstance {
    private final String serviceId;
    private final String host;
    private final int port;
}
