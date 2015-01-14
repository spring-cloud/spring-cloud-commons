/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.client.loadbalancer;

import java.net.URI;

import org.springframework.cloud.client.ServiceInstance;

/**
 * @author Spencer Gibb
 */
public interface LoadBalancerClient {
	/**
	 * Choose a ServiceInstance from the LoadBalancer for the specified service
	 * @param serviceId the service id to look up the LoadBalancer
	 * @return a ServiceInstance that matches the serviceId
	 */
	public ServiceInstance choose(String serviceId);

	/**
	 * execute request using a ServiceInstance from the LoadBalancer for the specified
	 * service
	 * @param serviceId the service id to look up the LoadBalancer
	 * @param request allows implementations to execute pre and post actions such as
	 * incrementing metrics
	 * @return the result of the LoadBalancerRequest callback on the selected
	 * ServiceInstance
	 */
	public <T> T execute(String serviceId, LoadBalancerRequest<T> request);

	public URI reconstructURI(ServiceInstance instance, URI original);

}
