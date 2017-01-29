package org.springframework.cloud.client.discovery;

import org.springframework.cloud.client.ServiceInstance;

import java.util.List;
import java.util.Map;

/**
 * Abstract implementation of DiscoveryClient to ease introduction of new methods in the future. This will avoid having
 * to update all implements every time a new method is added to the interface.
 */
public abstract class AbstractDiscoveryClient implements DiscoveryClient {

    /** Default implementation which ignores metadata for unsupported DiscoveryClient's */
    public List<ServiceInstance> getInstances(String serviceId, Map<String, String> metadata) {
        return getInstances(serviceId);
    }

}
