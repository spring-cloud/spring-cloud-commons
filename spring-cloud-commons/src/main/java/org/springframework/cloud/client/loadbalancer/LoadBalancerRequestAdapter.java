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

import org.springframework.cloud.client.ServiceInstance;

/**
 * An adapter class that allows creating {@link Request} objects from previously
 * {@link LoadBalancerRequest} objects.
 *
 * @author Olga Maciaszek-Sharma
 * @since 3.0.0
 */
public class LoadBalancerRequestAdapter<T, RC> extends DefaultRequest<RC> implements LoadBalancerRequest<T> {

	private final LoadBalancerRequest<T> delegate;

	public LoadBalancerRequestAdapter(LoadBalancerRequest<T> delegate) {
		this.delegate = delegate;
	}

	public LoadBalancerRequestAdapter(LoadBalancerRequest<T> delegate, RC context) {
		super(context);
		this.delegate = delegate;
	}

	@Override
	public T apply(ServiceInstance instance) throws Exception {
		return delegate.apply(instance);
	}

}
