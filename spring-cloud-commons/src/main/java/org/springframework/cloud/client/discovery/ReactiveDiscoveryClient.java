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

package org.springframework.cloud.client.discovery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.Ordered;

/**
 * Represents read operations commonly available to discovery services such as Netflix
 * Eureka or consul.io.
 *
 * @author Tim Ysewyn
 * @author Olga Maciaszek-Sharma
 */
public interface ReactiveDiscoveryClient extends Ordered {

	Log LOG = LogFactory.getLog(ReactiveDiscoveryClient.class);

	/**
	 * Default order of the discovery client.
	 */
	int DEFAULT_ORDER = 0;

	/**
	 * A human-readable description of the implementation, used in HealthIndicator.
	 * @return The description.
	 */
	String description();

	/**
	 * Gets all ServiceInstances associated with a particular serviceId.
	 * @param serviceId The serviceId to query.
	 * @return A List of ServiceInstance.
	 */
	Flux<ServiceInstance> getInstances(String serviceId);

	/**
	 * @return All known service IDs.
	 */
	Flux<String> getServices();

	/**
	 * Can be used to verify the client is still valid and able to make calls.
	 * <p>
	 * A successful invocation with no exception thrown implies the client is able to make
	 * calls.
	 * <p>
	 * The default implementation simply calls {@link #getServices()} - client
	 * implementations can override with a lighter weight operation if they choose to.
	 * @deprecated in favour of {@link ReactiveDiscoveryClient#reactiveProbe()}. This
	 * method should not be used as is, as it contains a bug - the method called within
	 * returns a {@link Flux}, which is not accessible for subscription or blocking from
	 * within. We are leaving it with a deprecation in order not to bring downstream
	 * implementations.
	 */
	@Deprecated
	default void probe() {
		if (LOG.isWarnEnabled()) {
			LOG.warn("ReactiveDiscoveryClient#probe has been called. If you're calling this method directly, "
					+ "use ReactiveDiscoveryClient#reactiveProbe instead.");
		}
		getServices();
	}

	/**
	 * Can be used to verify the client is still valid and able to make calls.
	 * <p>
	 * A successful invocation with no exception thrown implies the client is able to make
	 * calls.
	 * <p>
	 * The default implementation simply calls {@link #getServices()} and wraps it with a
	 * {@link Mono} - client implementations can override with a lighter weight operation
	 * if they choose to.
	 */
	default Mono<Void> reactiveProbe() {
		return getServices().then();
	}

	/**
	 * Default implementation for getting order of discovery clients.
	 * @return order
	 */
	@Override
	default int getOrder() {
		return DEFAULT_ORDER;
	}

}
