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

package org.springframework.cloud.loadbalancer.core;

import reactor.core.publisher.Flux;

import org.springframework.cloud.client.ServiceInstance;

/**
 * A no-op implementation of {@link ServiceInstanceSupplier}.
 *
 * @author Olga Maciaszek-Sharma
 * @deprecated Use {@link NoopServiceInstanceListSupplier} instead.
 */
@Deprecated
public class NoopServiceInstanceSupplier implements ServiceInstanceSupplier {

	@Override
	public String getServiceId() {
		return "";
	}

	@Override
	public Flux<ServiceInstance> get() {
		return Flux.empty();
	}

}
