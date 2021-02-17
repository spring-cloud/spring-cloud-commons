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

package org.springframework.cloud.client.loadbalancer;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class containing methods that allow to filter out supported
 * {@link LoadBalancerLifecycle} beans.
 *
 * @author Olga Maciaszek-Sharma
 * @since 3.0
 */
public final class LoadBalancerLifecycleValidator {

	private LoadBalancerLifecycleValidator() {
		throw new IllegalStateException("Can't instantiate a utility class");
	}

	@SuppressWarnings("rawtypes")
	public static Set<LoadBalancerLifecycle> getSupportedLifecycleProcessors(
			Map<String, LoadBalancerLifecycle> lifecycleProcessors, Class requestContextClass,
			Class clientResponseClass, Class serverTypeClass) {
		if (lifecycleProcessors == null) {
			return new HashSet<>();
		}
		return lifecycleProcessors.values().stream()
				.filter(lifecycle -> lifecycle.supports(requestContextClass, clientResponseClass, serverTypeClass))
				.collect(Collectors.toSet());
	}

}
