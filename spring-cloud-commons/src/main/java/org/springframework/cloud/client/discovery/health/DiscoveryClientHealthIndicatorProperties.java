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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Spencer Gibb
 */
@ConfigurationProperties("spring.cloud.discovery.client.health-indicator")
public class DiscoveryClientHealthIndicatorProperties {

	private boolean enabled = true;

	private boolean includeDescription = false;

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isIncludeDescription() {
		return this.includeDescription;
	}

	public void setIncludeDescription(boolean includeDescription) {
		this.includeDescription = includeDescription;
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("DiscoveryClientHealthIndicatorProperties{");
		sb.append("enabled=").append(this.enabled);
		sb.append(", includeDescription=").append(this.includeDescription);
		sb.append('}');
		return sb.toString();
	}

}
