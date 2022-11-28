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

import org.springframework.util.ClassUtils;

/**
 * Verifies if Spring Cloud Sleuth is present on the classpath.
 */
class SleuthPresentVerifier implements CompatibilityVerifier {

	private static final String TRACER_CLASS = "org.springframework.cloud.sleuth.Tracer";

	private static final String ERROR_DESCRIPTION = "Spring Cloud Sleuth is not compatible with this Spring Cloud release train";

	private static final String ACTION = """
						Migrate from Spring Cloud Sleuth to Micrometer Tracing .
						You can check the Sleuth 3.1 Migration Guide over here [https://github.com/micrometer-metrics/tracing/wiki/Spring-Cloud-Sleuth-3.1-Migration-Guide].\s
						If you want to disable this check, just set the property [spring.cloud.compatibility-verifier.enabled=false]""";

	@Override
	public VerificationResult verify() {
		boolean present = sleuthPresent();
		if (!present) {
			return VerificationResult.compatible();
		}
		return VerificationResult.notCompatible(ERROR_DESCRIPTION, ACTION);
	}

	boolean sleuthPresent() {
		return ClassUtils.isPresent(TRACER_CLASS, null);
	}

}
