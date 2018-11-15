package org.springframework.cloud.configuration;

import java.util.List;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Analyzer for the {@link CompatibilityNotMetException}. Prints a list of found
 * issues and actions that should be taken to fix them.
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
		return String.format("Your project setup is incompatible with our requirements "
						+ "due to following reasons:%s", descriptions(ex.results));
	}

	private String descriptions(List<VerificationResult> results) {
		StringBuilder builder = new StringBuilder("\n\n");
		for (VerificationResult result : results) {
			builder.append("- ").append(result.description).append("\n");
		}
		return builder.toString();
	}

	private String getAction(CompatibilityNotMetException ex) {
		return String.format("Consider applying the following actions:%s",
				actions(ex.results));
	}

	private String actions(List<VerificationResult> results) {
		StringBuilder builder = new StringBuilder("\n\n");
		for (VerificationResult result : results) {
			builder.append("- ").append(result.action).append("\n");
		}
		return builder.toString();
	}
}
