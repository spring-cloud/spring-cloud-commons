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

package org.springframework.cloud.client.discovery.simple.reactive;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import reactor.core.publisher.Flux;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;

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
public class SimpleReactiveDiscoveryProperties {

	private Map<String, List<DefaultServiceInstance>> instances = new HashMap<>();

	/**
	 * The properties of the local instance (if it exists). Users should set these
	 * properties explicitly if they are exporting data (e.g. metrics) that need to be
	 * identified by the service instance.
	 */
	private DefaultServiceInstance local = new DefaultServiceInstance();

	private int order = DiscoveryClient.DEFAULT_ORDER;

	public Flux<ServiceInstance> getInstances(String service) {
		return Flux.fromIterable(instances.getOrDefault(service, emptyList()));
	}

	Map<String, List<DefaultServiceInstance>> getInstances() {
		return instances;
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

}
