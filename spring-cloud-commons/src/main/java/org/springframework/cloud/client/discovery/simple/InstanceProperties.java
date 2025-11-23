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

package org.springframework.cloud.client.discovery.simple;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;

public class InstanceProperties {

	private @Nullable String instanceId;

	private @Nullable String serviceId;

	private @Nullable String host;

	private int port;

	private boolean secure;

	private Map<String, String> metadata = new LinkedHashMap<>();

	private @Nullable URI uri;

	public @Nullable String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(@Nullable String instanceId) {
		this.instanceId = instanceId;
	}

	public @Nullable String getServiceId() {
		return serviceId;
	}

	public void setServiceId(@Nullable String serviceId) {
		this.serviceId = serviceId;
	}

	public @Nullable String getHost() {
		return host;
	}

	public void setHost(@Nullable String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public boolean isSecure() {
		return secure;
	}

	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}

	public @Nullable URI getUri() {
		return uri;
	}

	public void setUri(@Nullable URI uri) {
		this.uri = uri;
		this.host = this.uri.getHost();
		this.port = this.uri.getPort();
		String scheme = this.uri.getScheme();
		if ("https".equals(scheme)) {
			this.secure = true;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		InstanceProperties instanceProperties = (InstanceProperties) o;
		return port == instanceProperties.port && secure == instanceProperties.secure
				&& Objects.equals(instanceId, instanceProperties.instanceId)
				&& Objects.equals(serviceId, instanceProperties.serviceId)
				&& Objects.equals(host, instanceProperties.host)
				&& Objects.equals(metadata, instanceProperties.metadata) && Objects.equals(uri, instanceProperties.uri);
	}

	@Override
	public int hashCode() {
		return Objects.hash(instanceId, serviceId, host, port, secure, metadata, uri);
	}

	@Override
	public String toString() {
		return new ToStringCreator(this).append("instanceId", instanceId)
			.append("serviceId", serviceId)
			.append("host", host)
			.append("port", port)
			.append("secure", secure)
			.append("metadata", metadata)
			.append("uri", uri)
			.toString();
	}

	public ServiceInstance toServiceInstance() {
		Assert.notNull(serviceId, "serviceId is required");
		Assert.notNull(host, "host is required");
		return new DefaultServiceInstance(instanceId, serviceId, host, port, secure, metadata);
	}

}
