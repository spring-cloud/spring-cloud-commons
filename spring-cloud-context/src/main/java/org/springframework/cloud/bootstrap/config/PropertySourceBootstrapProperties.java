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

package org.springframework.cloud.bootstrap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for Spring Cloud Config bootstrap.
 *
 * @author Dave Syer
 */
@ConfigurationProperties("spring.cloud.config")
public class PropertySourceBootstrapProperties {

	/**
	 * Flag to indicate that the external properties should override system properties.
	 * Default true.
	 */
	private boolean overrideSystemProperties = true;

	/**
	 * Flag to indicate that {@link #isOverrideSystemProperties()
	 * systemPropertiesOverride} can be used. Set to false to prevent users from changing
	 * the default accidentally. Default true.
	 */
	private boolean allowOverride = true;

	/**
	 * Flag to indicate that when {@link #setAllowOverride(boolean) allowOverride} is
	 * true, external properties should take lowest priority and should not override any
	 * existing property sources (including local config files). Default false.
	 */
	private boolean overrideNone = false;

	public boolean isOverrideNone() {
		return this.overrideNone;
	}

	public void setOverrideNone(boolean overrideNone) {
		this.overrideNone = overrideNone;
	}

	public boolean isOverrideSystemProperties() {
		return this.overrideSystemProperties;
	}

	public void setOverrideSystemProperties(boolean overrideSystemProperties) {
		this.overrideSystemProperties = overrideSystemProperties;
	}

	public boolean isAllowOverride() {
		return this.allowOverride;
	}

	public void setAllowOverride(boolean allowOverride) {
		this.allowOverride = allowOverride;
	}

}
