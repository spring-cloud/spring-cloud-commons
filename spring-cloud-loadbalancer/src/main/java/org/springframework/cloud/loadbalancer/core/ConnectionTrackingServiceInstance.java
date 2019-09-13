package org.springframework.cloud.loadbalancer.core;

import org.springframework.cloud.client.ServiceInstance;

/**
 * @author Olga Maciaszek-Sharma
 */
public interface ConnectionTrackingServiceInstance extends ServiceInstance {

	int getConnectionCount();

	void addConnection();

}
