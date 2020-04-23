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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Iterates over {@link CompatibilityVerifier} and prepares a report if exceptions were
 * found.
 */
class CompositeCompatibilityVerifier {

	private static final Log log = LogFactory
			.getLog(CompositeCompatibilityVerifier.class);

	private final List<CompatibilityVerifier> verifiers;

	CompositeCompatibilityVerifier(List<CompatibilityVerifier> verifiers) {
		this.verifiers = verifiers;
	}

	void verifyDependencies() {
		List<VerificationResult> errors = verifierErrors();
		if (errors.isEmpty()) {
			if (log.isDebugEnabled()) {
				log.debug("All conditions are passing");
			}
			return;
		}
		throw new CompatibilityNotMetException(errors);
	}

	private List<VerificationResult> verifierErrors() {
		List<VerificationResult> errors = new ArrayList<>();
		for (CompatibilityVerifier verifier : this.verifiers) {
			VerificationResult result = verifier.verify();
			if (result.isNotCompatible()) {
				errors.add(result);
			}
		}
		return errors;
	}

}
