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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Marcin Grzejszczak
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class CompatibilityVerifierAutoConfigurationTests {

	@Autowired
	MyCompatibilityVerifier myMismatchVerifier;

	@Test
	public void contextLoads() {
		then(this.myMismatchVerifier.called).isTrue();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	static class TestConfiguration {

		@Bean
		MyCompatibilityVerifier myMismatchVerifier() {
			return new MyCompatibilityVerifier();
		}

	}

	private static class MyCompatibilityVerifier implements CompatibilityVerifier {

		boolean called;

		@Override
		public VerificationResult verify() {
			this.called = true;
			return VerificationResult.compatible();
		}

	}

}
