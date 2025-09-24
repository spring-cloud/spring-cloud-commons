package org.springframework.cloud.loadbalancer.core;

import java.util.List;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import org.springframework.web.accept.ApiVersionDeprecationHandler;
import org.springframework.web.accept.ApiVersionParser;
import org.springframework.web.accept.ApiVersionResolver;
import org.springframework.web.accept.DefaultApiVersionStrategy;

/**
 * @author Olga Maciaszek-Sharma
 */
public class LoadBalancerApiVersionStrategy extends DefaultApiVersionStrategy {


	public LoadBalancerApiVersionStrategy(List<ApiVersionResolver> versionResolvers,
			ApiVersionParser<?> versionParser, boolean versionRequired, @Nullable String defaultVersion,
			boolean detectSupportedVersions, @Nullable Predicate<Comparable<?>> supportedVersionPredicate,
			@Nullable ApiVersionDeprecationHandler deprecationHandler) {
		super(versionResolvers, versionParser, versionRequired, defaultVersion, detectSupportedVersions,
				supportedVersionPredicate, deprecationHandler);
	}






}
