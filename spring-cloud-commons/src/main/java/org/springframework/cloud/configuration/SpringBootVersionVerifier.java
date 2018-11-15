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
package org.springframework.cloud.configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.util.StringUtils;

/**
 * Verifies if Spring Boot has proper version
 */
class SpringBootVersionVerifier implements CompatibilityVerifier {

	private static final Log log = LogFactory.getLog(SpringBootVersionVerifier.class);

	final Map<String, CompatibilityPredicate> ACCEPTED_VERSIONS = new HashMap<String, CompatibilityPredicate>() {
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
			return VerificationResult.compatible();
		}
		return VerificationResult.notCompatible(errorDescription(), action());
	}

	CompatibilityPredicate is1_5() {
		return new CompatibilityPredicate() {
			@Override
			public boolean isCompatible() {
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

	CompatibilityPredicate is2_0() {
		return new CompatibilityPredicate() {
			@Override
			public boolean isCompatible() {
				try {
					// present in 2.0, 1.5 missing in 2.1
					SpringApplicationBuilder.class.getMethod("web", boolean.class);
					return !is1_5().isCompatible();
				}
				catch (NoSuchMethodException e) {
					return false;
				}
			}
		};
	}

	CompatibilityPredicate is2_1() {
		return new CompatibilityPredicate() {
			@Override
			public boolean isCompatible() {
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
		String versionFromManifest = getVersionFromManifest();
		if (StringUtils.hasText(versionFromManifest)) {
			return String.format("Spring Boot in an incompatible version [%s] for this release train was found", versionFromManifest);
		}
		return "Spring Boot in an incompatible version for this release train was found";
	}

	private String action() {
		return String.format("Change Spring Boot version to one of the following versions %s, where e.g. [2.0] "
				+ "means Spring Boot in version [2.0.x] and [x] is the latest patch version of Spring Boot (e.g. 2.0.6.RELEASE).\n"
						+ "You can find the latest Spring Boot versions here [%s]. \n"
						+ "If you want to learn more about the Spring Cloud Release train compatibility, you "
						+ "can visit this page [%s] and check the [Release Trains] section.",
				this.acceptedVersions, "https://spring.io/projects/spring-boot#learn", "https://spring.io/projects/spring-cloud#overview");
	}

	private boolean springBootVersionMatches() {
		for (String acceptedVersion : acceptedVersions) {
			if (bootVersionFromManifest(acceptedVersion)) {
				return true;
			}
			else {
				// 2.0, 2.1
				CompatibilityPredicate predicate = ACCEPTED_VERSIONS.get(acceptedVersion);
				if (predicate != null && predicate.isCompatible()) {
					return true;
				}
			}
		}
		return false;
	}
}
