/*
 * Copyright 2013-2017 the original author or authors.
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Default implementation of {@link ServiceInstance}.
 *
 * @author Spencer Gibb
 */
public class DefaultServiceInstance implements ServiceInstance {

	private final String serviceId;

	private final String host;

	private final int port;

	private final boolean secure;

	private final Map<String, String> metadata;

	public DefaultServiceInstance(String serviceId, String host, int port, boolean secure,
			Map<String, String> metadata) {
		this.serviceId = serviceId;
		this.host = host;
		this.port = port;
		this.secure = secure;
		this.metadata = metadata;
	}

	public DefaultServiceInstance(String serviceId, String host, int port,
			boolean secure) {
		this(serviceId, host, port, secure, new LinkedHashMap<String, String>());
	}

	@Override
	public URI getUri() {
		return getUri(this);
	}

	@Override
	public Map<String, String> getMetadata() {
		return this.metadata;
	}

	/**
	 * Create a uri from the given ServiceInstance's host:port
	 * @param instance
	 * @return URI of the form (secure)?https:http + "host:port"
	 */
	public static URI getUri(ServiceInstance instance) {
		String scheme = (instance.isSecure()) ? "https" : "http";
		String uri = String.format("%s://%s:%s", scheme, instance.getHost(),
				instance.getPort());
		return URI.create(uri);
	}

	@Override
	public String getServiceId() {
		return serviceId;
	}

	@Override
	public String getHost() {
		return host;
	}

	@Override
	public int getPort() {
		return port;
	}

	@Override
	public boolean isSecure() {
		return secure;
	}

	@Override
	public String toString() {
		return "DefaultServiceInstance{" +
				"serviceId='" + serviceId + '\'' +
				", host='" + host + '\'' +
				", port=" + port +
				", secure=" + secure +
				", metadata=" + metadata +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DefaultServiceInstance that = (DefaultServiceInstance) o;
		return port == that.port &&
				secure == that.secure &&
				Objects.equals(serviceId, that.serviceId) &&
				Objects.equals(host, that.host) &&
				Objects.equals(metadata, that.metadata);
	}

	@Override
	public int hashCode() {
		return Objects.hash(serviceId, host, port, secure, metadata);
	}
}
