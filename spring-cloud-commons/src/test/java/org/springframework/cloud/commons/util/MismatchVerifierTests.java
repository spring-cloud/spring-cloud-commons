package org.springframework.cloud.commons.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.BDDAssertions;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.test.rule.OutputCapture;

/**
 * @author Marcin Grzejszczak
 */
public class MismatchVerifierTests {

	@Rule public OutputCapture outputCapture = new OutputCapture();

	@Test
	public void should_not_print_the_report_when_no_errors_were_found() {
		CompositeMismatchVerifier verifier = new CompositeMismatchVerifier(new ArrayList<MismatchVerifier>());

		verifier.verifyDependencies();

		BDDAssertions.then(outputCapture.toString()).doesNotContain("SPRING CLOUD VERIFICATION FAILED");
	}

	@Test
	public void should_print_the_report_when_errors_were_found() {
		List<MismatchVerifier> list = new ArrayList<>();
		list.add(new MismatchVerifier() {
			@Override
			public VerificationResult verify() {
				return new VerificationResult("Some description of error1");
			}
		});
		list.add(new MismatchVerifier() {
			@Override
			public VerificationResult verify() {
				return new VerificationResult("Some description of error2");
			}
		});
		CompositeMismatchVerifier verifier = new CompositeMismatchVerifier(list);

		try {
			verifier.verifyDependencies();
			BDDAssertions.fail("should fail");
		} catch (IllegalStateException ex) {

		}

		BDDAssertions.then(outputCapture.toString())
				.contains("SPRING CLOUD VERIFICATION FAILED")
				.contains("Some description of error1")
				.contains("Some description of error2");
	}

}