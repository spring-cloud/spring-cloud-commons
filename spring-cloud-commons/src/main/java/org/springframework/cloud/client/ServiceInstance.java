/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.cloud.client;

import java.net.URI;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Represents an instance of a service in a discovery system.
 *
 * @author Spencer Gibb
 * @author Tim Ysewyn
 */
public interface ServiceInstance {

	/**
	 * @return The unique instance ID as registered.
	 */
	default @Nullable String getInstanceId() {
		return null;
	}

	/**
	 * @return The service ID as registered.
	 */
	String getServiceId();

	/**
	 * @return The hostname of the registered service instance.
	 */
	String getHost();

	/**
	 * @return The port of the registered service instance.
	 */
	int getPort();

	/**
	 * @return Whether the port of the registered service instance uses HTTPS.
	 */
	boolean isSecure();

	/**
	 * @return The service URI address.
	 */
	URI getUri();

	/**
	 * @return The key / value pair metadata associated with the service instance.
	 */
	@Nullable Map<String, String> getMetadata();

	/**
	 * @return The scheme of the service instance.
	 */
	default @Nullable String getScheme() {
		return null;
	}

	/**
	 * Creates a URI from the given ServiceInstance's host:port.
	 * @param instance the ServiceInstance.
	 * @return URI of the form (secure)?https:http + "host:port". Scheme port default used
	 * if port not set.
	 */
	static URI createUri(ServiceInstance instance) {
		String scheme = (instance.isSecure()) ? "https" : "http";
		int port = instance.getPort();
		if (port <= 0) {
			port = (instance.isSecure()) ? 443 : 80;
		}
		String uri = String.format("%s://%s:%s", scheme, instance.getHost(), port);
		return URI.create(uri);
	}

}
