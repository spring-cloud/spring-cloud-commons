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

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.BDDAssertions;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.test.system.OutputCaptureRule;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Marcin Grzejszczak
 */
public class CompatibilityVerifierTests {

	@Rule
	public OutputCaptureRule outputCapture = new OutputCaptureRule();

	@Test
	public void should_not_print_the_report_when_no_errors_were_found() {
		CompositeCompatibilityVerifier verifier = new CompositeCompatibilityVerifier(
				new ArrayList<CompatibilityVerifier>());

		verifier.verifyDependencies();

		then(this.outputCapture.toString())
				.doesNotContain("SPRING CLOUD VERIFICATION FAILED");
	}

	@Test
	public void should_print_the_report_when_errors_were_found() {
		List<CompatibilityVerifier> list = new ArrayList<>();
		list.add(new CompatibilityVerifier() {
			@Override
			public VerificationResult verify() {
				return VerificationResult.notCompatible("Wrong Boot version",
						"Use Boot version 1.2");
			}
		});
		list.add(new CompatibilityVerifier() {
			@Override
			public VerificationResult verify() {
				return VerificationResult.notCompatible("Wrong JDK version",
						"Use JDK 25");
			}
		});
		CompositeCompatibilityVerifier verifier = new CompositeCompatibilityVerifier(
				list);

		try {
			verifier.verifyDependencies();
			BDDAssertions.fail("should fail");
		}
		catch (CompatibilityNotMetException ex) {
			then(ex.results).hasSize(2);
		}
	}

}
