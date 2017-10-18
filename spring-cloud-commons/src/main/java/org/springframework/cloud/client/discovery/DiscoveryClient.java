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

package org.springframework.cloud.client.discovery;

import java.util.List;

import org.springframework.cloud.client.ServiceInstance;

/**
 * DiscoveryClient represents read operations commonly available to Discovery service such as
 * Netflix Eureka or consul.io
 * @author Spencer Gibb
 */
public interface DiscoveryClient {

	/**
	 * A human readable description of the implementation, used in HealthIndicator
	 * @return the description
	 */
	String description();

	/**
	 * Get all ServiceInstances associated with a particular serviceId
	 * @param serviceId the serviceId to query
	 * @return a List of ServiceInstance
	 */
	List<ServiceInstance> getInstances(String serviceId);

	/**
	 * @return all known service ids
	 */
	List<String> getServices();

}
