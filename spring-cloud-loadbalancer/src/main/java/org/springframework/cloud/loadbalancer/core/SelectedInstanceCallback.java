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

package org.springframework.cloud.loadbalancer.core;

import org.springframework.cloud.client.ServiceInstance;

/**
 * A callback interface that allows to pass the selected service instance data from the
 * LoadBalancer.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.7
 */
public interface SelectedInstanceCallback {

	/**
	 * Passes the selected {@link ServiceInstance} as an argument.
	 * @param serviceInstance that has been selected
	 */
	void selectedServiceInstance(ServiceInstance serviceInstance);

}
