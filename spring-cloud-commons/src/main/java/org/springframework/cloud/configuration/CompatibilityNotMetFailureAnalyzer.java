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

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Analyzer for the {@link CompatibilityNotMetException}. Prints a list of found issues
 * and actions that should be taken to fix them.
 *
 * @author Marcin Grzejszczak
 * @since 1.3.6
 */
public final class CompatibilityNotMetFailureAnalyzer extends AbstractFailureAnalyzer<CompatibilityNotMetException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, CompatibilityNotMetException cause) {
		return new FailureAnalysis(getDescription(cause), getAction(cause), cause);
	}

	private String getDescription(CompatibilityNotMetException ex) {
		return String.format(
				"Your project setup is incompatible with our requirements " + "due to following reasons:%s",
				descriptions(ex.results));
	}

	private String descriptions(List<VerificationResult> results) {
		StringBuilder builder = new StringBuilder("\n\n");
		for (VerificationResult result : results) {
			builder.append("- ").append(result.description).append("\n");
		}
		return builder.toString();
	}

	private String getAction(CompatibilityNotMetException ex) {
		return String.format("Consider applying the following actions:%s", actions(ex.results));
	}

	private String actions(List<VerificationResult> results) {
		StringBuilder builder = new StringBuilder("\n\n");
		for (VerificationResult result : results) {
			builder.append("- ").append(result.action).append("\n");
		}
		return builder.toString();
	}

}
