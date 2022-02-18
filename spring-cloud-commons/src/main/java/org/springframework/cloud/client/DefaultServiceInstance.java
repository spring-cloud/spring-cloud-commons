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
 * @author Charu Covindane
 * @author Neil Powell
 */
public class DefaultServiceInstance implements ServiceInstance {

	private String instanceId;

	private String serviceId;

	private String host;

	private int port;

	private boolean secure;

	private Map<String, String> metadata = new LinkedHashMap<>();

	private URI uri;

	/**
	 * @param instanceId the id of the instance.
	 * @param serviceId the id of the service.
	 * @param host the host where the service instance can be found.
	 * @param port the port on which the service is running.
	 * @param secure indicates whether or not the connection needs to be secure.
	 * @param metadata a map containing metadata.
	 */
	public DefaultServiceInstance(String instanceId, String serviceId, String host, int port, boolean secure,
			Map<String, String> metadata) {
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
	public DefaultServiceInstance(String instanceId, String serviceId, String host, int port, boolean secure) {
		this(instanceId, serviceId, host, port, secure, new LinkedHashMap<>());
	}

	public DefaultServiceInstance() {
	}

	/**
	 * Creates a URI from the given ServiceInstance's host:port.
	 * @param instance the ServiceInstance.
	 * @return URI of the form (secure)?https:http + "host:port". Scheme port default used
	 * if port not set.
	 */
	public static URI getUri(ServiceInstance instance) {
		String scheme = (instance.isSecure()) ? "https" : "http";
		int port = instance.getPort();
		if (port <= 0) {
			port = (instance.isSecure()) ? 443 : 80;
		}
		String uri = String.format("%s://%s:%s", scheme, instance.getHost(), port);
		return URI.create(uri);
	}

	@Override
	public URI getUri() {
		return getUri(this);
	}

	@Override
	public Map<String, String> getMetadata() {
		return metadata;
	}

	@Override
	public String getInstanceId() {
		return instanceId;
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

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setUri(URI uri) {
		this.uri = uri;
		this.host = this.uri.getHost();
		this.port = this.uri.getPort();
		String scheme = this.uri.getScheme();
		if ("https".equals(scheme)) {
			this.secure = true;
		}
	}

	@Override
	public String toString() {
		return "DefaultServiceInstance{" + "instanceId='" + instanceId + '\'' + ", serviceId='" + serviceId + '\''
				+ ", host='" + host + '\'' + ", port=" + port + ", secure=" + secure + ", metadata=" + metadata + '}';
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
		return port == that.port && secure == that.secure && Objects.equals(instanceId, that.instanceId)
				&& Objects.equals(serviceId, that.serviceId) && Objects.equals(host, that.host)
				&& Objects.equals(metadata, that.metadata);
	}

	@Override
	public int hashCode() {
		return Objects.hash(instanceId, serviceId, host, port, secure, metadata);
	}

}
