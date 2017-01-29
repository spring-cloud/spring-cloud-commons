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
import java.util.Map;

import org.springframework.cloud.client.ServiceInstance;

/**
 * DiscoveryClient represents operations commonly available to Discovery service such as
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
	 * @deprecated use the {@link org.springframework.cloud.client.serviceregistry.Registration} bean instead
	 *
	 * @return ServiceInstance with information used to register the local service
	 */
	ServiceInstance getLocalServiceInstance();

	/**
	 * Get all ServiceInstances associated with a particular serviceId
	 * @param serviceId the serviceId to query
	 * @return a List of ServiceInstance
	 */
	List<ServiceInstance> getInstances(String serviceId);

	/**
	 * Get all ServiceInstances associated with a particular serviceId that match the provided metadata
	 *
	 * Note that not all implementations support metadata, so some or all of the metadata provided may
	 * be ignored when looking up a service.
	 *
	 * @param serviceId the serviceId to query
	 * @param metadata metadata to use in constraining the search
	 * @return a List of ServiceInstance
	 */
	List<ServiceInstance> getInstances(String serviceId, Map<String, String> metadata);

	/**
	 * @return all known service ids
	 */
	List<String> getServices();

}
