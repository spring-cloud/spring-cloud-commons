/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.client.loadbalancer;

import java.io.IOException;
import java.net.URI;

import org.springframework.cloud.client.ServiceInstance;

/**
 * Represents a client-side load balancer.
 *
 * @author Spencer Gibb
 */
public interface LoadBalancerClient extends ServiceInstanceChooser {

	/**
	 * Executes request using a ServiceInstance from the LoadBalancer for the specified
	 * service.
	 * @param serviceId The service ID to look up the LoadBalancer.
	 * @param request Allows implementations to execute pre and post actions, such as
	 * incrementing metrics.
	 * @param <T> type of the response
	 * @throws IOException in case of IO issues.
	 * @return The result of the LoadBalancerRequest callback on the selected
	 * ServiceInstance.
	 */
	<T> T execute(String serviceId, LoadBalancerRequest<T> request) throws IOException;

	/**
	 * Executes request using a ServiceInstance from the LoadBalancer for the specified
	 * service.
	 * @param serviceId The service ID to look up the LoadBalancer.
	 * @param serviceInstance The service to execute the request to.
	 * @param request Allows implementations to execute pre and post actions, such as
	 * incrementing metrics.
	 * @param <T> type of the response
	 * @throws IOException in case of IO issues.
	 * @return The result of the LoadBalancerRequest callback on the selected
	 * ServiceInstance.
	 */
	<T> T execute(String serviceId, ServiceInstance serviceInstance, LoadBalancerRequest<T> request) throws IOException;

	/**
	 * Creates a proper URI with a real host and port for systems to utilize. Some systems
	 * use a URI with the logical service name as the host, such as
	 * http://myservice/path/to/service. This will replace the service name with the
	 * host:port from the ServiceInstance.
	 * @param instance service instance to reconstruct the URI
	 * @param original A URI with the host as a logical service name.
	 * @return A reconstructed URI.
	 */
	URI reconstructURI(ServiceInstance instance, URI original);

}
