package org.springframework.cloud.commons.util;

import java.util.Collections;
import java.util.List;

import org.assertj.core.api.BDDAssertions;
import org.junit.Test;

/**
 * @author Marcin Grzejszczak
 */
public class SpringBootDependencyTests {

	@Test
	public void should_read_concrete_version_from_manifest() {
		List<String> acceptedVersions = Collections.singletonList("2.0.3.RELEASE");
		SpringBootVersionVerifier
				versionVerifier = new SpringBootVersionVerifier(acceptedVersions) {
			@Override
			String getVersionFromManifest() {
				return "2.0.3.RELEASE";
			}
		};
		versionVerifier.ACCEPTED_VERSIONS.clear();

		VerificationResult verificationResult = versionVerifier.verify();

		BDDAssertions.then(verificationResult.description).isEmpty();
	}

	@Test
	public void should_read_concrete_version_from_manifest_and_return_false_when_version_is_not_matched() {
		List<String> acceptedVersions = Collections.singletonList("2.0.9.RELEASE");
		SpringBootVersionVerifier
				versionVerifier = new SpringBootVersionVerifier(acceptedVersions) {
			@Override
			String getVersionFromManifest() {
				return "2.0.3.RELEASE";
			}
		};
		versionVerifier.ACCEPTED_VERSIONS.clear();

		VerificationResult verificationResult = versionVerifier.verify();

		BDDAssertions.then(verificationResult.description).isNotEmpty();
	}

	@Test
	public void should_read_concrete_version_from_manifest_and_return_false_when_minor_version_is_not_matched() {
		List<String> acceptedVersions = Collections.singletonList("2.0");
		SpringBootVersionVerifier
				versionVerifier = new SpringBootVersionVerifier(acceptedVersions) {
			@Override
			String getVersionFromManifest() {
				return "2.1.3.RELEASE";
			}
		};
		versionVerifier.ACCEPTED_VERSIONS.clear();

		VerificationResult verificationResult = versionVerifier.verify();

		BDDAssertions.then(verificationResult.description).isNotEmpty();
	}

	@Test
	public void should_read_concrete_version_from_manifest_and_match_it_against_minor_version() {
		List<String> acceptedVersions = Collections.singletonList("2.0");
		SpringBootVersionVerifier
				versionVerifier = new SpringBootVersionVerifier(acceptedVersions) {
			@Override
			String getVersionFromManifest() {
				return "2.0.3.RELEASE";
			}
		};
		versionVerifier.ACCEPTED_VERSIONS.clear();

		VerificationResult verificationResult = versionVerifier.verify();

		BDDAssertions.then(verificationResult.description).isEmpty();
	}

	@Test
	public void should_match_against_predicate() {
		List<String> acceptedVersions = Collections.singletonList("2.5");
		SpringBootVersionVerifier
				versionVerifier = new SpringBootVersionVerifier(acceptedVersions) {
			@Override
			String getVersionFromManifest() {
				return "";
			}
		};
		versionVerifier.ACCEPTED_VERSIONS.clear();
		versionVerifier.ACCEPTED_VERSIONS.put("2.5", new MismatchPredicate() {
			@Override
			public boolean accept() {
				return true;
			}
		});

		VerificationResult verificationResult = versionVerifier.verify();

		BDDAssertions.then(verificationResult.description).isEmpty();
	}

	@Test
	public void should_fail_to_match_against_predicate_when_none_is_matching() {
		List<String> acceptedVersions = Collections.singletonList("2.5");
		SpringBootVersionVerifier
				versionVerifier = new SpringBootVersionVerifier(acceptedVersions) {
			@Override
			String getVersionFromManifest() {
				return "";
			}
		};
		versionVerifier.ACCEPTED_VERSIONS.clear();

		VerificationResult verificationResult = versionVerifier.verify();

		BDDAssertions.then(verificationResult.description).isNotEmpty();
	}

	@Test
	public void should_match_against_current_manifest() {
		List<String> acceptedVersions = Collections.singletonList("1.5");
		SpringBootVersionVerifier
				versionVerifier = new SpringBootVersionVerifier(acceptedVersions);
		versionVerifier.ACCEPTED_VERSIONS.clear();

		VerificationResult verificationResult = versionVerifier.verify();

		BDDAssertions.then(verificationResult.description).isEmpty();
	}

	@Test
	public void should_match_against_current_predicate() {
		List<String> acceptedVersions = Collections.singletonList("1.5");
		SpringBootVersionVerifier
				versionVerifier = new SpringBootVersionVerifier(acceptedVersions){
			@Override
			String getVersionFromManifest() {
				return "";
			}
		};
		versionVerifier.ACCEPTED_VERSIONS.clear();
		versionVerifier.ACCEPTED_VERSIONS.put("1.5", versionVerifier.is1_5());

		VerificationResult verificationResult = versionVerifier.verify();

		BDDAssertions.then(verificationResult.description).isEmpty();
	}

	@Test
	public void should_fail_to_match_against_predicate_for_non_current_versions() {
		List<String> acceptedVersions = Collections.singletonList("1.5");
		SpringBootVersionVerifier
				versionVerifier = new SpringBootVersionVerifier(acceptedVersions) {
			@Override
			String getVersionFromManifest() {
				return "";
			}
		};
		versionVerifier.ACCEPTED_VERSIONS.remove("1.5");

		VerificationResult verificationResult = versionVerifier.verify();

		BDDAssertions.then(verificationResult.description).isNotEmpty();
	}

}