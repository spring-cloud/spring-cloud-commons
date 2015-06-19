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

package org.springframework.cloud.client;

import java.net.URI;
import java.util.Set;

/**
 * Represents an instance of a Service in a Discovery System
 * @author Spencer Gibb
 */
public interface ServiceInstance {
	/**
	 * Enum of known service capability types.
	 * @author Matt Benson
	 */
	public enum Capability {
		/**
		 * Capability to return a {@link Set} of {@link String} which comprise a set of
		 * tags to identify a given service instance.
		 */
		TAGS;
	}

	/**
	 * @return the service id as register by the DiscoveryClient
	 */
	public String getServiceId();

	/**
	 * @return the hostname of the registered ServiceInstance
	 */
	public String getHost();

	/**
	 * @return the port of the registered ServiceInstance
	 */
	public int getPort();

	/**
	 * @return ifthe port of the registered ServiceInstance is https or not
	 */
	public boolean isSecure();

	public URI getUri();

	/**
	 * @param capability
	 * @return whether the specified service capability is supported by this
	 * ServiceInstance.
	 */
	public boolean supports(Capability capability);

	/**
	 * @param capability
	 * @return service capability payload value (using type inference)
	 */
	public <T> T getValue(Capability capability);
}
