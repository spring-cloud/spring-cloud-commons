/*
 * Copyright 2013-2021 the original author or authors.
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

package org.springframework.cloud.client.loadbalancer;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * A {@link ConfigurationProperties} bean for Spring Cloud Loadbalancer.
 *
 * Individual clients are configured via the {@link LoadBalancerClientsProperties#clients}
 * field. Defaults and other properties are located in the {@link LoadBalancerProperties}
 * base class.
 *
 * @author Spencer Gibb
 * @since 3.1.0
 */
@ConfigurationProperties("spring.cloud.loadbalancer")
public class LoadBalancerClientsProperties extends LoadBalancerProperties {

	private Map<String, LoadBalancerProperties> clients = new HashMap<>();

	public Map<String, LoadBalancerProperties> getClients() {
		return this.clients;
	}

}
