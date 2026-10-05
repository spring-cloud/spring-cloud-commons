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

package org.springframework.cloud.client.discovery.composite.reactive;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for reactive composite discovery client.
 *
 * @author hutiefang76
 */
@ConfigurationProperties("spring.cloud.discovery.reactive")
public class ReactiveCompositeDiscoveryClientProperties {

	/**
	 * Enables reactive discovery.
	 */
	private boolean enabled = true;

	/**
	 * Waits for reactive discovery clients in order and falls back only when a client
	 * returns no instances, instead of using the fastest non-empty response.
	 */
	private boolean orderEnforced = false;

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isOrderEnforced() {
		return this.orderEnforced;
	}

	public void setOrderEnforced(boolean orderEnforced) {
		this.orderEnforced = orderEnforced;
	}

}
