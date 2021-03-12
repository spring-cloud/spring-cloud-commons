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

import java.util.Objects;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.style.ToStringCreator;

/**
 * A request context object that allows storing information on previously used service
 * instances.
 *
 * @author Olga Maciaszek-Sharma
 */
public class RetryableRequestContext extends RequestDataContext {

	private ServiceInstance previousServiceInstance;

	public RetryableRequestContext(ServiceInstance previousServiceInstance) {
		this.previousServiceInstance = previousServiceInstance;
	}

	public RetryableRequestContext(ServiceInstance previousServiceInstance, RequestData clientRequestData) {
		super(clientRequestData);
		this.previousServiceInstance = previousServiceInstance;
	}

	public RetryableRequestContext(ServiceInstance previousServiceInstance, RequestData clientRequestData,
			String hint) {
		super(clientRequestData, hint);
		this.previousServiceInstance = previousServiceInstance;
	}

	public ServiceInstance getPreviousServiceInstance() {
		return previousServiceInstance;
	}

	public void setPreviousServiceInstance(ServiceInstance previousServiceInstance) {
		this.previousServiceInstance = previousServiceInstance;
	}

	@Override
	public String toString() {
		ToStringCreator to = new ToStringCreator(this);
		to.append("previousServiceInstance", previousServiceInstance);
		return to.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof RetryableRequestContext)) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		RetryableRequestContext context = (RetryableRequestContext) o;
		return Objects.equals(previousServiceInstance, context.previousServiceInstance);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), previousServiceInstance);
	}

}
