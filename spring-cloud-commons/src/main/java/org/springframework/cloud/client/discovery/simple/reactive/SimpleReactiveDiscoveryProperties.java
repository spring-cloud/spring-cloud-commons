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

package org.springframework.cloud.client.discovery.simple.reactive;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.client.discovery.simple.InstanceProperties;

import static java.util.Collections.emptyList;

/**
 * Properties to hold the details of a {@link ReactiveDiscoveryClient} service instance
 * for a given service. It also holds the user-configurable order that will be used to
 * establish the precedence of this client in the list of clients used by
 * {@link org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClient}.
 *
 * @author Tim Ysewyn
 * @author Charu Covindane
 * @since 2.2.0
 */
@ConfigurationProperties(prefix = "spring.cloud.discovery.client.simple")
public class SimpleReactiveDiscoveryProperties implements InitializingBean {

	private Map<String, List<InstanceProperties>> instances = new HashMap<>();

	/**
	 * The properties of the local instance (if it exists). Users should set these
	 * properties explicitly if they are exporting data (e.g. metrics) that need to be
	 * identified by the service instance.
	 */
	@NestedConfigurationProperty
	private InstanceProperties local = new InstanceProperties();

	private int order = DiscoveryClient.DEFAULT_ORDER;

	public List<InstanceProperties> getInstances(String service) {
		return instances.getOrDefault(service, emptyList());
	}

	Map<String, List<InstanceProperties>> getInstances() {
		return instances;
	}

	public void setInstances(Map<String, List<InstanceProperties>> instances) {
		this.instances = instances;
	}

	public InstanceProperties getLocal() {
		return this.local;
	}

	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public void afterPropertiesSet() {
		for (String key : this.instances.keySet()) {
			for (InstanceProperties instance : this.instances.get(key)) {
				instance.setServiceId(key);
			}
		}
	}

}
