/*
 * Copyright 2012-2019 the original author or authors.
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

import org.assertj.core.api.BDDAssertions;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.NestedExceptionUtils;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Marcin Grzejszczak
 */
public class CompatibilityVerifierFailureAutoConfigurationTests {

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	@Test
	public void contextFailsToLoad() {
		try {
			SpringApplication.run(TestConfiguration.class,
					"--spring.cloud.compatibility-verifier.compatible-boot-versions=1.2.x,1.3.x");
			BDDAssertions.fail("should throw exception");
		}
		catch (BeanCreationException ex) {
			Throwable cause = NestedExceptionUtils.getRootCause(ex);
			then(((CompatibilityNotMetException) cause).results).hasSize(1);
		}
	}

	@Configuration
	@EnableAutoConfiguration
	static class TestConfiguration {

	}

}
