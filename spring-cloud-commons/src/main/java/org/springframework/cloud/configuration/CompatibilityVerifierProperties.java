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

package org.springframework.cloud.configuration;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Marcin Grzejszczak
 */
@ConfigurationProperties("spring.cloud.compatibility-verifier")
public class CompatibilityVerifierProperties {

	/**
	 * Enables creation of Spring Cloud compatibility verification.
	 */
	private boolean enabled;

	/**
	 * Default accepted versions for the Spring Boot dependency. You can set {@code x} for
	 * the patch version if you don't want to specify a concrete value. Example:
	 * {@code 3.4.x}
	 */
	private List<String> compatibleBootVersions = Arrays.asList("2.2.x", "2.3.x");

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public List<String> getCompatibleBootVersions() {
		return this.compatibleBootVersions;
	}

	public void setCompatibleBootVersions(List<String> compatibleBootVersions) {
		this.compatibleBootVersions = compatibleBootVersions;
	}

}
