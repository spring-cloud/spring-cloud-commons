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

import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;

class SleuthPresentVerifierTests {

	@Test
	void should_be_compatible_when_sleuth_is_not_on_the_classpath() {
		VerificationResult verify = new SleuthPresentVerifier() {
			@Override
			boolean sleuthPresent() {
				return false;
			}
		}.verify();

		BDDAssertions.then(verify.isNotCompatible()).isFalse();
	}

	@Test
	void should_not_be_compatible_when_sleuth_is_on_the_classpath() {
		VerificationResult verify = new SleuthPresentVerifier() {
			@Override
			boolean sleuthPresent() {
				return true;
			}
		}.verify();

		BDDAssertions.then(verify.isNotCompatible()).isTrue();

	}

}
