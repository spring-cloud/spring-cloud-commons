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
import java.util.Map;

/**
 * Represents an instance of a Service in a Discovery System
 *
 * @author Spencer Gibb
 * @author Steven van Beelen
 */
public interface ServiceInstance {

	/**
	 * @return the service id as registered.
	 */
	String getServiceId();

	/**
	 * @return the hostname of the registered ServiceInstance
	 */
	String getHost();

	/**
	 * @return the port of the registered ServiceInstance
	 */
	int getPort();

	/**
	 * @return if the port of the registered ServiceInstance is https or not
	 */
	boolean isSecure();

	/**
	 * @return the service uri address
	 */
	URI getUri();

	/**
	 * @return the key value pair metadata associated with the service instance
	 */
	Map<String, String> getMetadata();

	/**
	 * Add a key/value pair to the metadata map.
     *
	 * @param key   a metadata key as a String
	 * @param value a corresponding metadata value as a String
	 */
	default void putMetadata(String key, String value) {
	}

	/**
	 * Replace the current metadata map for a new one.
     *
	 * @param metadata a metadata map to replace the current metadata
	 */
	default void setMetadata(Map<String, String> metadata) {
	}

}
