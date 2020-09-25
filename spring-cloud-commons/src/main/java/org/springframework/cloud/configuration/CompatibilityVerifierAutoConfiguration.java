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

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} that fails the build fast for
 * incompatible versions of dependencies (e.g. invalid version of Boot).
 *
 * @author Marcin Grzejszczak
 * @since 1.3.6
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "spring.cloud.compatibility-verifier.enabled", matchIfMissing = true)
@AutoConfigureOrder(0)
@EnableConfigurationProperties(CompatibilityVerifierProperties.class)
public class CompatibilityVerifierAutoConfiguration {

	@Bean
	CompositeCompatibilityVerifier compositeCompatibilityVerifier(List<CompatibilityVerifier> verifiers) {
		CompositeCompatibilityVerifier verifier = new CompositeCompatibilityVerifier(verifiers);
		verifier.verifyDependencies();
		return verifier;
	}

	@Bean
	SpringBootVersionVerifier springBootVersionVerifier(CompatibilityVerifierProperties properties) {
		return new SpringBootVersionVerifier(properties.getCompatibleBootVersions());
	}

}
