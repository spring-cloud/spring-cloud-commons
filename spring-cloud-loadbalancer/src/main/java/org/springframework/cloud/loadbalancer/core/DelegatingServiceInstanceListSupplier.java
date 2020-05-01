/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.function.Supplier;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.util.Assert;

/**
 * @author Spencer Gibb
 */
public abstract class DelegatingServiceInstanceListSupplier
		implements ServiceInstanceListSupplier {

	private final ServiceInstanceListSupplier delegate;

	public DelegatingServiceInstanceListSupplier(ServiceInstanceListSupplier delegate) {
		Assert.notNull(delegate, "delegate may not be null");
		this.delegate = delegate;
	}

	public ServiceInstanceListSupplier getDelegate() {
		return this.delegate;
	}

	@Override
	public String getServiceId() {
		return this.delegate.getServiceId();
	}
}
