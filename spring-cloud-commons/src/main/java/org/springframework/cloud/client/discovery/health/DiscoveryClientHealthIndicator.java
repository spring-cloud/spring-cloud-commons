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

package org.springframework.cloud.client.discovery.health;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;

/**
 * @author Spencer Gibb
 * @author Chris Bono
 */
public class DiscoveryClientHealthIndicator implements DiscoveryHealthIndicator, Ordered,
		ApplicationListener<InstanceRegisteredEvent<?>> {

	private final ObjectProvider<DiscoveryClient> discoveryClient;

	private final DiscoveryClientHealthIndicatorProperties properties;

	private final Log log = LogFactory.getLog(DiscoveryClientHealthIndicator.class);

	private AtomicBoolean discoveryInitialized = new AtomicBoolean(false);

	private int order = Ordered.HIGHEST_PRECEDENCE;

	public DiscoveryClientHealthIndicator(ObjectProvider<DiscoveryClient> discoveryClient,
			DiscoveryClientHealthIndicatorProperties properties) {
		this.discoveryClient = discoveryClient;
		this.properties = properties;
	}

	@Override
	public void onApplicationEvent(InstanceRegisteredEvent<?> event) {
		if (this.discoveryInitialized.compareAndSet(false, true)) {
			this.log.debug("Discovery Client has been initialized");
		}
	}

	@Override
	public Health health() {
		Health.Builder builder = new Health.Builder();

		if (this.discoveryInitialized.get()) {
			try {
				DiscoveryClient client = this.discoveryClient.getIfAvailable();
				String description = (this.properties.isIncludeDescription())
						? client.description() : "";

				if (properties.isUseServicesQuery()) {
					List<String> services = client.getServices();
					builder.status(new Status("UP", description)).withDetail("services",
							services);
				}
				else {
					client.probe();
					builder.status(new Status("UP", description));
				}
			}
			catch (Exception e) {
				this.log.error("Error", e);
				builder.down(e);
			}
		}
		else {
			builder.status(new Status(Status.UNKNOWN.getCode(),
					"Discovery Client not initialized"));
		}
		return builder.build();
	}

	@Override
	public String getName() {
		return "discoveryClient";
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

}
