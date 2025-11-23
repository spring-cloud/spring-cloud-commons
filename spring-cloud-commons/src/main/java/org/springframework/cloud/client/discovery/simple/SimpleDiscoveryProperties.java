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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.style.ToStringCreator;

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
public class SimpleDiscoveryProperties implements InitializingBean {

	private Map<String, List<InstanceProperties>> instances = new HashMap<>();

	/**
	 * The properties of the local instance (if it exists). Users should set these
	 * properties explicitly if they are exporting data (e.g. metrics) that need to be
	 * identified by the service instance.
	 */
	@NestedConfigurationProperty
	private InstanceProperties local = new InstanceProperties();

	private int order = DiscoveryClient.DEFAULT_ORDER;

	public Map<String, List<InstanceProperties>> getInstances() {
		return this.instances;
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

	@Override
	public String toString() {
		return new ToStringCreator(this).append("instances", instances)
			.append("local", local)
			.append("order", order)
			.toString();
	}

}
