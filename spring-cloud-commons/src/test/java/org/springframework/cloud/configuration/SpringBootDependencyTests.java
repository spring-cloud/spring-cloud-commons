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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Marcin Grzejszczak
 */
public class SpringBootDependencyTests {

	@Test
	public void should_read_concrete_version_from_manifest() {
		List<String> acceptedVersions = Collections.singletonList("2.1.3.RELEASE");
		SpringBootVersionVerifier versionVerifier = new SpringBootVersionVerifier(
				acceptedVersions) {
			@Override
			String getVersionFromManifest() {
				return "2.1.3.RELEASE";
			}
		};
		versionVerifier.ACCEPTED_VERSIONS.clear();

		VerificationResult verificationResult = versionVerifier.verify();

		then(verificationResult.description).isEmpty();
		then(verificationResult.action).isEmpty();
	}

	@Test
	public void should_read_concrete_version_from_manifest_and_return_false_when_version_is_not_matched() {
		List<String> acceptedVersions = Collections.singletonList("2.1.9.RELEASE");
		SpringBootVersionVerifier versionVerifier = new SpringBootVersionVerifier(
				acceptedVersions) {
			@Override
			String getVersionFromManifest() {
				return "2.1.3.RELEASE";
			}
		};
		versionVerifier.ACCEPTED_VERSIONS.clear();

		VerificationResult verificationResult = versionVerifier.verify();

		then(verificationResult.description).isNotEmpty();
		then(verificationResult.action).isNotEmpty();
	}

	@Test
	public void should_read_concrete_version_from_manifest_and_return_false_when_minor_version_is_not_matched() {
		List<String> acceptedVersions = Collections.singletonList("2.1");
		SpringBootVersionVerifier versionVerifier = new SpringBootVersionVerifier(
				acceptedVersions) {
			@Override
			String getVersionFromManifest() {
				return "2.99.3.RELEASE";
			}
		};
		versionVerifier.ACCEPTED_VERSIONS.clear();

		VerificationResult verificationResult = versionVerifier.verify();

		then(verificationResult.description).isNotEmpty();
		then(verificationResult.action).isNotEmpty();
	}

	@Test
	public void should_read_concrete_version_from_manifest_and_match_it_against_minor_version() {
		List<String> acceptedVersions = Collections.singletonList("2.1");
		SpringBootVersionVerifier versionVerifier = new SpringBootVersionVerifier(
				acceptedVersions) {
			@Override
			String getVersionFromManifest() {
				return "2.1.3.RELEASE";
			}
		};
		versionVerifier.ACCEPTED_VERSIONS.clear();

		VerificationResult verificationResult = versionVerifier.verify();

		then(verificationResult.description).isEmpty();
		then(verificationResult.action).isEmpty();
	}

	@Test
	public void should_match_against_predicate() {
		List<String> acceptedVersions = Collections.singletonList("2.5");
		SpringBootVersionVerifier versionVerifier = new SpringBootVersionVerifier(
				acceptedVersions) {
			@Override
			String getVersionFromManifest() {
				return "";
			}
		};
		versionVerifier.ACCEPTED_VERSIONS.clear();
		versionVerifier.ACCEPTED_VERSIONS.put("2.5", new CompatibilityPredicate() {
			@Override
			public boolean isCompatible() {
				return true;
			}
		});

		VerificationResult verificationResult = versionVerifier.verify();

		then(verificationResult.description).isEmpty();
		then(verificationResult.action).isEmpty();
	}

	@Test
	public void should_fail_to_match_against_predicate_when_none_is_matching() {
		List<String> acceptedVersions = Collections.singletonList("2.5");
		SpringBootVersionVerifier versionVerifier = new SpringBootVersionVerifier(
				acceptedVersions) {
			@Override
			String getVersionFromManifest() {
				return "2.1";
			}
		};
		versionVerifier.ACCEPTED_VERSIONS.clear();

		VerificationResult verificationResult = versionVerifier.verify();

		then(verificationResult.description).isNotEmpty();
		then(verificationResult.action).isNotEmpty();
	}

	@Test
	public void should_not_match_when_manifest_has_version_and_not_compatible() {
		List<String> acceptedVersions = Collections.singletonList("2.5");
		SpringBootVersionVerifier versionVerifier = new SpringBootVersionVerifier(
				acceptedVersions) {
			@Override
			String getVersionFromManifest() {
				return "2.1";
			}
		};
		versionVerifier.ACCEPTED_VERSIONS.clear();
		AtomicBoolean verifierRun = new AtomicBoolean(false);
		versionVerifier.ACCEPTED_VERSIONS.put("2.5",
				() -> verifierRun.compareAndSet(false, true));

		VerificationResult verificationResult = versionVerifier.verify();

		then(verifierRun).isFalse();
		then(verificationResult.description).isNotEmpty();
		then(verificationResult.action).isNotEmpty();
	}

	@Test
	public void should_match_when_manifest_has_version_and_compatible_list() {
		List<String> acceptedVersions = Arrays.asList("2.0", "2.1");
		SpringBootVersionVerifier versionVerifier = new SpringBootVersionVerifier(
				acceptedVersions) {
			@Override
			String getVersionFromManifest() {
				return "2.1";
			}
		};
		versionVerifier.ACCEPTED_VERSIONS.clear();
		AtomicBoolean verifierRun = new AtomicBoolean(false);
		versionVerifier.ACCEPTED_VERSIONS.put("2.5",
				() -> verifierRun.compareAndSet(false, true));

		VerificationResult verificationResult = versionVerifier.verify();

		then(verifierRun).isFalse();
		then(verificationResult.description).isEmpty();
		then(verificationResult.action).isEmpty();
	}

	@Ignore // FIXME: https://github.com/spring-cloud/spring-cloud-commons/issues/717
	@Test
	public void should_match_against_current_manifest() {
		verifyCurrentVersionFromManifest("2.3");
		verifyCurrentVersionFromManifest("2.3.x");
	}

	private void verifyCurrentVersionFromManifest(String version) {
		List<String> acceptedVersions = Collections.singletonList(version);
		SpringBootVersionVerifier versionVerifier = new SpringBootVersionVerifier(
				acceptedVersions);
		versionVerifier.ACCEPTED_VERSIONS.clear();

		VerificationResult verificationResult = versionVerifier.verify();

		then(verificationResult.description).isEmpty();
		then(verificationResult.action).isEmpty();
	}

	@Test
	public void should_match_against_current_predicate() {
		List<String> acceptedVersions = Collections.singletonList("2.1");
		SpringBootVersionVerifier versionVerifier = new SpringBootVersionVerifier(
				acceptedVersions) {
			@Override
			String getVersionFromManifest() {
				return "";
			}
		};
		versionVerifier.ACCEPTED_VERSIONS.clear();
		versionVerifier.ACCEPTED_VERSIONS.put("2.1", versionVerifier.is2_1());

		VerificationResult verificationResult = versionVerifier.verify();

		then(verificationResult.description).isEmpty();
		then(verificationResult.action).isEmpty();
	}

	@Test
	public void should_match_against_current_predicate_with_version_ending_with_x() {
		List<String> acceptedVersions = Collections.singletonList("2.1.x");
		SpringBootVersionVerifier versionVerifier = new SpringBootVersionVerifier(
				acceptedVersions) {
			@Override
			String getVersionFromManifest() {
				return "";
			}
		};
		versionVerifier.ACCEPTED_VERSIONS.clear();
		versionVerifier.ACCEPTED_VERSIONS.put("2.1", versionVerifier.is2_1());

		VerificationResult verificationResult = versionVerifier.verify();

		then(verificationResult.description).isEmpty();
		then(verificationResult.action).isEmpty();
	}

	@Test
	public void should_fail_to_match_against_predicate_for_non_current_versions() {
		List<String> acceptedVersions = Collections.singletonList("2.1");
		SpringBootVersionVerifier versionVerifier = new SpringBootVersionVerifier(
				acceptedVersions) {
			@Override
			String getVersionFromManifest() {
				return "2.0";
			}
		};
		versionVerifier.ACCEPTED_VERSIONS.remove("2.1");

		VerificationResult verificationResult = versionVerifier.verify();

		then(verificationResult.description).isNotEmpty();
		then(verificationResult.action).isNotEmpty();
	}

}
