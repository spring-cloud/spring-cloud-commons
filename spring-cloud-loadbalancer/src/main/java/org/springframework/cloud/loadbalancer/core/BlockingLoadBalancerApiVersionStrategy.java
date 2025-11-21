/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.cloud.loadbalancer.core;

import java.util.List;
import java.util.function.Predicate;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.web.accept.ApiVersionDeprecationHandler;
import org.springframework.web.accept.ApiVersionParser;
import org.springframework.web.accept.ApiVersionResolver;
import org.springframework.web.accept.DefaultApiVersionStrategy;
import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.accept.MissingApiVersionException;

/**
 * A LoadBalancer-specific implementation of {@link DefaultApiVersionStrategy}. It
 * overrides the
 * {@link DefaultApiVersionStrategy#validateVersion(Comparable, HttpServletRequest)}
 * method in order to skip any version validation.
 *
 * @author Olga Maciaszek-Sharma
 * @since 5.0.0
 */
public class BlockingLoadBalancerApiVersionStrategy extends DefaultApiVersionStrategy {

	private static final ApiVersionParser<String> EMPTY_API_VERSION_PARSER = version -> {
		throw new InvalidApiVersionException("No valid ApiVersionParserFound: " + version);
	};

	public BlockingLoadBalancerApiVersionStrategy(List<ApiVersionResolver> versionResolvers,
			@Nullable ApiVersionParser<?> versionParser, @Nullable Boolean versionRequired,
			@Nullable String defaultVersion, boolean detectSupportedVersions,
			@Nullable Predicate<Comparable<?>> supportedVersionPredicate,
			@Nullable ApiVersionDeprecationHandler deprecationHandler) {
		super(versionResolvers, (versionParser != null) ? versionParser : EMPTY_API_VERSION_PARSER, versionRequired,
				defaultVersion, detectSupportedVersions, supportedVersionPredicate, deprecationHandler);
	}

	@Override
	public void validateVersion(@Nullable Comparable<?> requestVersion, HttpServletRequest request)
			throws MissingApiVersionException, InvalidApiVersionException {
		// Do nothing
	}

}
