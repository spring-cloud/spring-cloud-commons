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

package org.springframework.cloud.configuration;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Marcin Grzejszczak
 */
@ExtendWith(OutputCaptureExtension.class)
public class CompatibilityVerifierTests {

	@Test
	public void should_not_print_the_report_when_no_errors_were_found(CapturedOutput output) {
		CompositeCompatibilityVerifier verifier = new CompositeCompatibilityVerifier(new ArrayList<>());

		verifier.verifyDependencies();

		then(output).doesNotContain("SPRING CLOUD VERIFICATION FAILED");
	}

	@Test
	public void should_print_the_report_when_errors_were_found() {
		List<CompatibilityVerifier> list = new ArrayList<>();
		list.add(() -> VerificationResult.notCompatible("Wrong Boot version", "Use Boot version 1.2"));
		list.add(() -> VerificationResult.notCompatible("Wrong JDK version", "Use JDK 25"));
		CompositeCompatibilityVerifier verifier = new CompositeCompatibilityVerifier(list);

		try {
			verifier.verifyDependencies();
			BDDAssertions.fail("should fail");
		}
		catch (CompatibilityNotMetException ex) {
			then(ex.results).hasSize(2);
		}
	}

}
