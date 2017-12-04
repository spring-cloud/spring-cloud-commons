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

package org.springframework.cloud.client.discovery.health;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;


/**
 * @author Spencer Gibb
 */
public class DiscoveryClientHealthIndicator implements DiscoveryHealthIndicator, Ordered,
		ApplicationListener<InstanceRegisteredEvent<?>> {

	private AtomicBoolean discoveryInitialized = new AtomicBoolean(false);

	private int order = Ordered.HIGHEST_PRECEDENCE;

	private final DiscoveryClient discoveryClient;
	private final DiscoveryClientHealthIndicatorProperties properties;

	private final Log log = LogFactory.getLog(DiscoveryClientHealthIndicator.class);

	@Deprecated
	public DiscoveryClientHealthIndicator(DiscoveryClient discoveryClient) {
		this(discoveryClient, new DiscoveryClientHealthIndicatorProperties());
	}

	public DiscoveryClientHealthIndicator(DiscoveryClient discoveryClient, DiscoveryClientHealthIndicatorProperties properties) {
		this.discoveryClient = discoveryClient;
		this.properties = properties;
	}

	@Override
	public void onApplicationEvent(InstanceRegisteredEvent<?> event) {
		if (this.discoveryInitialized.compareAndSet(false, true)) {
			log.debug("Discovery Client has been initialized");
		}
	}

	@Override
	public Health health() {
		Health.Builder builder = new Health.Builder();

		if (this.discoveryInitialized.get()) {
			try {
				List<String> services = this.discoveryClient.getServices();
				String description = (this.properties.isIncludeDescription()) ? this.discoveryClient.description() : "";
				builder.status(new Status("UP", description))
						.withDetail("services", services);
			}
			catch (Exception e) {
				log.error("Error", e);
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
