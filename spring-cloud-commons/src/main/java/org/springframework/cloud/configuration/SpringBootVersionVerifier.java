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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringBootVersion;
import org.springframework.util.StringUtils;

/**
 * Verifies if Spring Boot has proper version.
 */
class SpringBootVersionVerifier implements CompatibilityVerifier {

	private static final Log log = LogFactory.getLog(SpringBootVersionVerifier.class);

	final Map<String, CompatibilityPredicate> ACCEPTED_VERSIONS = new HashMap<>() {
		{
			this.put("3.0", is3_0());
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
			return VerificationResult.compatible();
		}
		return VerificationResult.notCompatible(errorDescription(), action());
	}

	private Boolean bootVersionFromManifest(String s) {
		String version = getVersionFromManifest();
		if (log.isDebugEnabled()) {
			log.debug("Version found in Boot manifest [" + version + "]");
		}
		if (!StringUtils.hasText(version)) {
			log.info("Cannot check Boot version from manifest");
			return null;
		}
		return version.startsWith(stripWildCardFromVersion(s));
	}

	String getVersionFromManifest() {
		return SpringBootVersion.getVersion();
	}

	CompatibilityPredicate is3_0() {
		return new CompatibilityPredicate() {

			@Override
			public String toString() {
				return "Predicate for Boot 3.0";
			}

			@Override
			public boolean isCompatible() {
				try {
					// since 3.0
					Class.forName(
							"org.springframework.boot.autoconfigure.validation.ValidationConfigurationCustomizer");
					return true;

				}
				catch (ClassNotFoundException e) {
					return false;
				}

			}
		};
	}

	private String errorDescription() {
		String versionFromManifest = getVersionFromManifest();
		if (StringUtils.hasText(versionFromManifest)) {
			return String.format("Spring Boot [%s] is not compatible with this Spring Cloud release train",
					versionFromManifest);
		}
		return "Spring Boot is not compatible with this Spring Cloud release train";
	}

	private String action() {
		return String.format(
				"""
						Change Spring Boot version to one of the following versions %s .
						You can find the latest Spring Boot versions here [%s].\s
						If you want to learn more about the Spring Cloud Release train compatibility, you can visit this page [%s] and check the [Release Trains] section.
						If you want to disable this check, just set the property [spring.cloud.compatibility-verifier.enabled=false]""",
				this.acceptedVersions, "https://spring.io/projects/spring-boot#learn",
				"https://spring.io/projects/spring-cloud#overview");
	}

	private boolean springBootVersionMatches() {
		for (String acceptedVersion : this.acceptedVersions) {
			Boolean versionFromManifest = bootVersionFromManifest(acceptedVersion);
			// if manifest has version and matches, return
			// otherwise need to check other versions in list
			// if all return false, then the return false at end will apply
			if (versionFromManifest != null && versionFromManifest) {
				return true;
			}
			else if (versionFromManifest == null) {
				// only check these if the manifest does not have a version.
				// otherwise this could lead to false positives for future
				// versions of boot
				CompatibilityPredicate predicate = this.ACCEPTED_VERSIONS
						.get(stripWildCardFromVersion(acceptedVersion));
				if (predicate != null && predicate.isCompatible()) {
					if (log.isDebugEnabled()) {
						log.debug("Predicate [" + predicate + "] was matched");
					}
					return true;
				}
			}
		}
		return false;
	}

	static String stripWildCardFromVersion(String version) {
		if (version.endsWith(".x")) {
			return version.substring(0, version.indexOf(".x"));
		}
		return version;
	}

}
