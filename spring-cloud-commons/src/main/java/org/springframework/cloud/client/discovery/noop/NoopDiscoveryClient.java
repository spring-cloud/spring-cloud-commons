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

package org.springframework.cloud.client.discovery.noop;

import java.util.Collections;
import java.util.List;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

/**
 * DiscoveryClient used when no implementations are found on the classpath.
 *
 * @deprecated Use
 * {@link org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClient
 * instead}.
 * @author Dave Syer
 */

@Deprecated
public class NoopDiscoveryClient implements DiscoveryClient {

	public NoopDiscoveryClient(ServiceInstance instance) {
	}

	@Override
	public String description() {
		return "Spring Cloud No-op DiscoveryClient";
	}

	@Override
	public List<ServiceInstance> getInstances(String serviceId) {
		return Collections.emptyList();
	}

	@Override
	public List<String> getServices() {
		return Collections.emptyList();
	}

}
