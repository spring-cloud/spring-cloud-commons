/*
 * Copyright 2012-2023 the original author or authors.
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

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Represents a {@link ServiceInstanceListSupplier} that uses a delegate
 * {@link ServiceInstanceListSupplier} instance underneath.
 *
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
public abstract class DelegatingServiceInstanceListSupplier
		implements ServiceInstanceListSupplier, InitializingBean, DisposableBean {

	protected final ServiceInstanceListSupplier delegate;

	public DelegatingServiceInstanceListSupplier(ServiceInstanceListSupplier delegate) {
		Assert.notNull(delegate, "delegate may not be null");
		this.delegate = delegate;
	}

	public ServiceInstanceListSupplier getDelegate() {
		return delegate;
	}

	@Override
	public String getServiceId() {
		return delegate.getServiceId();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (delegate instanceof InitializingBean) {
			((InitializingBean) delegate).afterPropertiesSet();
		}
	}

	@Override
	public void destroy() throws Exception {
		if (delegate instanceof DisposableBean) {
			((DisposableBean) delegate).destroy();
		}
	}

}
