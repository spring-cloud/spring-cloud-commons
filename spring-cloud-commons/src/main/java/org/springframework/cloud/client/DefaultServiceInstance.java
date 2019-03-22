/*
 * Copyright 2012-2019 the original author or authors.
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Default implementation of {@link ServiceInstance}.
 *
 * @author Spencer Gibb
 * @author Tim Ysewyn
 */
public class DefaultServiceInstance implements ServiceInstance {

	private final String instanceId;

	private final String serviceId;

	private final String host;

	private final int port;

	private final boolean secure;

	private final Map<String, String> metadata;

	/**
	 * @param instanceId the id of the instance.
	 * @param serviceId the id of the service.
	 * @param host the host where the service instance can be found.
	 * @param port the port on which the service is running.
	 * @param secure indicates whether or not the connection needs to be secure.
	 * @param metadata a map containing metadata.
	 */
	public DefaultServiceInstance(String instanceId, String serviceId, String host,
			int port, boolean secure, Map<String, String> metadata) {
		this.instanceId = instanceId;
		this.serviceId = serviceId;
		this.host = host;
		this.port = port;
		this.secure = secure;
		this.metadata = metadata;
	}

	/**
	 * @param instanceId the id of the instance.
	 * @param serviceId the id of the service.
	 * @param host the host where the service instance can be found.
	 * @param port the port on which the service is running.
	 * @param secure indicates whether or not the connection needs to be secure.
	 */
	public DefaultServiceInstance(String instanceId, String serviceId, String host,
			int port, boolean secure) {
		this(instanceId, serviceId, host, port, secure, new LinkedHashMap<>());
	}

	/**
	 * @param serviceId the id of the service.
	 * @param host the host where the service instance can be found.
	 * @param port the port on which the service is running.
	 * @param secure indicates whether or not the connection needs to be secure.
	 * @param metadata a map containing metadata.
	 * @deprecated - use other constructors
	 */
	@Deprecated
	public DefaultServiceInstance(String serviceId, String host, int port, boolean secure,
			Map<String, String> metadata) {
		this(null, serviceId, host, port, secure, metadata);
	}

	/**
	 * @param serviceId the id of the service.
	 * @param host the host where the service instance can be found.
	 * @param port the port on which the service is running.
	 * @param secure indicates whether or not the connection needs to be secure.
	 * @deprecated - use other constructors
	 */
	@Deprecated
	public DefaultServiceInstance(String serviceId, String host, int port,
			boolean secure) {
		this(serviceId, host, port, secure, new LinkedHashMap<>());
	}

	/**
	 * Creates a URI from the given ServiceInstance's host:port.
	 * @param instance the ServiceInstance.
	 * @return URI of the form (secure)?https:http + "host:port".
	 */
	public static URI getUri(ServiceInstance instance) {
		String scheme = (instance.isSecure()) ? "https" : "http";
		String uri = String.format("%s://%s:%s", scheme, instance.getHost(),
				instance.getPort());
		return URI.create(uri);
	}

	@Override
	public URI getUri() {
		return getUri(this);
	}

	@Override
	public Map<String, String> getMetadata() {
		return this.metadata;
	}

	@Override
	public String getInstanceId() {
		return this.instanceId;
	}

	@Override
	public String getServiceId() {
		return this.serviceId;
	}

	@Override
	public String getHost() {
		return this.host;
	}

	@Override
	public int getPort() {
		return this.port;
	}

	@Override
	public boolean isSecure() {
		return this.secure;
	}

	@Override
	public String toString() {
		return "DefaultServiceInstance{" + "instanceId='" + this.instanceId + '\''
				+ ", serviceId='" + this.serviceId + '\'' + ", host='" + this.host + '\''
				+ ", port=" + this.port + ", secure=" + this.secure + ", metadata="
				+ this.metadata + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DefaultServiceInstance that = (DefaultServiceInstance) o;
		return this.port == that.port && this.secure == that.secure
				&& Objects.equals(this.instanceId, that.instanceId)
				&& Objects.equals(this.serviceId, that.serviceId)
				&& Objects.equals(this.host, that.host)
				&& Objects.equals(this.metadata, that.metadata);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.instanceId, this.serviceId, this.host, this.port,
				this.secure, this.metadata);
	}

}
