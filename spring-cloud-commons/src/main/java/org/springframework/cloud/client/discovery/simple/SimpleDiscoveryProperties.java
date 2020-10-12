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

package org.springframework.cloud.client.discovery.simple;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

/**
 * Properties to hold the details of a
 * {@link org.springframework.cloud.client.discovery.DiscoveryClient} service instances
 * for a given service. It also holds the user-configurable order that will be used to
 * establish the precedence of this client in the list of clients used by
 * {@link org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClient}.
 *
 * @author Biju Kunjummen
 * @author Olga Maciaszek-Sharma
 * @author Tim Ysewyn
 * @author Charu Covindane
 */

@ConfigurationProperties(prefix = "spring.cloud.discovery.client.simple")
public class SimpleDiscoveryProperties {

	private Map<String, List<DefaultServiceInstance>> instances = new HashMap<>();

	/**
	 * The properties of the local instance (if it exists). Users should set these
	 * properties explicitly if they are exporting data (e.g. metrics) that need to be
	 * identified by the service instance.
	 */
	private DefaultServiceInstance local = new DefaultServiceInstance(null, null, null, 0,
			false);

	private int order = DiscoveryClient.DEFAULT_ORDER;

	public Map<String, List<DefaultServiceInstance>> getInstances() {
		return this.instances;
	}

	public void setInstances(Map<String, List<DefaultServiceInstance>> instances) {
		this.instances = instances;
	}

	public DefaultServiceInstance getLocal() {
		return this.local;
	}

	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@PostConstruct
	public void init() {
		for (String key : this.instances.keySet()) {
			for (DefaultServiceInstance instance : this.instances.get(key)) {
				instance.setServiceId(key);
			}
		}
	}

	public void setInstance(String serviceId, String host, int port) {
		local = new DefaultServiceInstance(null, serviceId, host, port, false);
	}

	/**
	 * Basic implementation of {@link ServiceInstance}.
	 *
	 * @deprecated in favor of {@link DefaultServiceInstance}
	 */
	@Deprecated
	public static class SimpleServiceInstance implements ServiceInstance {

		/**
		 * The URI of the service instance. Will be parsed to extract the scheme, host,
		 * and port.
		 */
		private URI uri;

		private String host;

		private int port;

		private boolean secure;

		/**
		 * Metadata for the service instance. Can be used by discovery clients to modify
		 * their behaviour per instance, e.g. when load balancing.
		 */
		private Map<String, String> metadata = new LinkedHashMap<>();

		/**
		 * The unique identifier or name for the service instance.
		 */
		private String instanceId;

		/**
		 * The identifier or name for the service. Multiple instances might share the same
		 * service ID.
		 */
		private String serviceId;

		public SimpleServiceInstance() {
		}

		public SimpleServiceInstance(URI uri) {
			setUri(uri);
		}

		@Override
		public String getInstanceId() {
			return this.instanceId;
		}

		public void setInstanceId(String id) {
			this.instanceId = id;
		}

		@Override
		public String getServiceId() {
			return this.serviceId;
		}

		public void setServiceId(String id) {
			this.serviceId = id;
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
		public URI getUri() {
			return this.uri;
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
		public Map<String, String> getMetadata() {
			return this.metadata;
		}

	}

}
