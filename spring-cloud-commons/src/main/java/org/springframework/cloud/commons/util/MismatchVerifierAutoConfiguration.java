/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.cloud.commons.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;


/**
 * {@link EnableAutoConfiguration Auto-configuration} that fails the build fast for incompatible
 * versions of dependencies (e.g. invalid version of Boot).
 *
 * @author Marcin Grzejszczak
 * @since 1.3.6
 */
@Configuration
@ConditionalOnProperty(value = "spring.cloud.util.mismatch-verifier.enabled", matchIfMissing = true)
@AutoConfigureOrder(0)
public class MismatchVerifierAutoConfiguration {

	@Bean
	CompositeMismatchVerifier compositeMismatchVerifier(List<MismatchVerifier> verifiers) {
		CompositeMismatchVerifier verifier = new CompositeMismatchVerifier(verifiers);
		verifier.verifyDependencies();
		return verifier;
	}

	@Bean
	SpringBootVersionVerifier springBootVersionVerifier(
			@Value("${spring.cloud.util.mismatch-verifier.compatible-boot-versions:1.5}") List<String> acceptedVersions) {
		return new SpringBootVersionVerifier(acceptedVersions);
	}


}

interface MismatchVerifier {
	VerificationResult verify();
}

// So that can be used for jdk7
interface MismatchPredicate {
	boolean accept();
}

/**
 * Iterates over {@link MismatchVerifier} and prepares a report if exceptions were found
 */
class CompositeMismatchVerifier {

	private static final Log log = LogFactory.getLog(CompositeMismatchVerifier.class);

	private final List<MismatchVerifier> verifiers;

	CompositeMismatchVerifier(List<MismatchVerifier> verifiers) {
		this.verifiers = verifiers;
	}

	void verifyDependencies() {
		List<String> errors = verifierErrors();
		if (errors.isEmpty()) {
			if (log.isDebugEnabled()) {
				log.debug("All conditions are passing");
			}
			return;
		}
		log.error(reportMessage(errors).toString());
		throw new IllegalStateException("SPRING CLOUD VERIFICATION FAILED");
	}

	DependencyMismatchReportMessage reportMessage(List<String> errors) {
		return new DependencyMismatchReportMessage(errors);
	}

	private List<String> verifierErrors() {
		List<String> errors = new ArrayList<>();
		for (MismatchVerifier verifier : this.verifiers) {
			VerificationResult result = verifier.verify();
			if (StringUtils.hasText(result.description)) {
				errors.add(result.description);
			}
		}
		return errors;
	}
}

/**
 * Verifies if Spring Boot has proper version
 */
class SpringBootVersionVerifier implements MismatchVerifier {

	private static final Log log = LogFactory.getLog(SpringBootVersionVerifier.class);

	final Map<String, MismatchPredicate> ACCEPTED_VERSIONS = new HashMap<String, MismatchPredicate>() {
		{
			this.put("1.5", is1_5());
			this.put("2.0", is2_0());
			this.put("2.1", is2_1());
		}
	};

	private final List<String> acceptedVersions;

	SpringBootVersionVerifier(List<String> acceptedVersions) {
		this.acceptedVersions = acceptedVersions;
	}

	@Override
	public VerificationResult verify() {
		boolean matches = springBootVersionMatches();
		if (matches) {
			return new VerificationResult();
		}
		return new VerificationResult(errorDescription());
	}

	MismatchPredicate is1_5() {
		return new MismatchPredicate() {
			@Override
			public boolean accept() {
				try {
					// deprecated 1.5
					Class.forName("org.springframework.boot.context.config.ResourceNotFoundException");
					return true;
				}
				catch (ClassNotFoundException e) {
					return false;
				}
			}
		};
	}

	private boolean bootVersionFromManifest(String s) {
		String version = getVersionFromManifest();
		if (log.isDebugEnabled()) {
			log.debug("Version found in Boot manifest [" + version + "]");
		}
		return StringUtils.hasText(version) && version.startsWith(s);
	}

	String getVersionFromManifest() {
		return SpringBootVersion.getVersion();
	}

	MismatchPredicate is2_0() {
		return new MismatchPredicate() {
			@Override
			public boolean accept() {
				try {
					// present in 2.0, 1.5 missing in 2.1
					SpringApplicationBuilder.class.getMethod("web", boolean.class);
					return !is1_5().accept();
				}
				catch (NoSuchMethodException e) {
					return false;
				}
			}
		};
	}

	MismatchPredicate is2_1() {
		return new MismatchPredicate() {
			@Override
			public boolean accept() {
				try {
					// since 2.1
					Class.forName("org.springframework.boot.task.TaskExecutorCustomizer");
					return true;
				}
				catch (ClassNotFoundException e) {
					return false;
				}
			}
		};
	}

	private String errorDescription() {
		return "You are using Spring Boot in version incompatible with compatible versions " + this.acceptedVersions;
	}

	private boolean springBootVersionMatches() {
		for (String acceptedVersion : acceptedVersions) {
			if (bootVersionFromManifest(acceptedVersion)) {
				return true;
			}
			else {
				// 2.0, 2.1
				MismatchPredicate predicate = ACCEPTED_VERSIONS.get(acceptedVersion);
				if (predicate != null && predicate.accept()) {
					return true;
				}
			}
		}
		return false;
	}
}

class VerificationResult {
	final String description;

	// if OK
	VerificationResult() {
		this.description = "";
	}

	// if not OK
	VerificationResult(String errorDescription) {
		this.description = errorDescription;
	}
}

class DependencyMismatchReportMessage {

	private StringBuilder message;

	DependencyMismatchReportMessage(List<String> descriptions) {
		this.message = getLogMessage(descriptions);
	}

	private StringBuilder getLogMessage(List<String> descriptions) {
		StringBuilder message = new StringBuilder();
		message.append(String.format("%n%n%n"));
		message.append(String.format("=================================%n"));
		message.append(String.format("SPRING CLOUD VERIFICATION FAILED%n"));
		message.append(String.format("=================================%n%n%n"));
		message.append(String.format("Exception messages:%n"));
		message.append(String.format("---------------------------------%n"));
		for (String description : descriptions) {
			message.append(description).append(String.format("%n"));
		}
		message.append(String.format("%n%n"));
		return message;
	}

	@Override
	public String toString() {
		return this.message.toString();
	}

}
