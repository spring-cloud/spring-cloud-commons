/*
 * Copyright 2013-2015 the original author or authors.
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

import lombok.extern.apachecommons.CommonsLog;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.Ordered;

/**
 * @author Spencer Gibb
 */
@CommonsLog
public class DiscoveryClientHealthIndicator implements DiscoveryHealthIndicator, Ordered {

	private int order = Ordered.HIGHEST_PRECEDENCE;

	private DiscoveryClient discoveryClient;

	public DiscoveryClientHealthIndicator(DiscoveryClient discoveryClient) {
		this.discoveryClient = discoveryClient;
	}

	@Override
	public Health health() {
		Health.Builder builder = new Health.Builder();
		try {
			List<String> services = this.discoveryClient.getServices();
			builder.status(new Status("UP", this.discoveryClient.description()))
					.withDetail("services", services);
		}
		catch (Exception e) {
			log.error("Error", e);
			builder.down(e);
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
