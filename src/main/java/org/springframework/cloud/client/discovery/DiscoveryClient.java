package org.springframework.cloud.client.discovery;

import org.springframework.cloud.client.ServiceInstance;

import java.util.List;

/**
 * @author Spencer Gibb
 */
//TODO: merge with LoadBalancerClient?
public interface DiscoveryClient {
	public String description();

    /**
     * @return ServiceInstance with information used to register the local service
     */
    public ServiceInstance getLocalServiceInstance();

    /**
     * Get all ServiceInstance's associated with a particular serviceId
     * @param serviceId the serviceId to query
     * @return a List of ServiceInstance
     */
    public List<ServiceInstance> getInstances(String serviceId);

    public List<ServiceInstance> getAllInstances();

    /**
     * @return all known service id's
     */
    public List<String> getServices();
}
