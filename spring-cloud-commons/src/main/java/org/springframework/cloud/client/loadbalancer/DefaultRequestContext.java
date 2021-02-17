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

import org.springframework.core.style.ToStringCreator;

/**
 * Contains information relevant to the request.
 *
 * @author Olga Maciaszek-Sharma
 */
public class DefaultRequestContext extends HintRequestContext {

	/**
	 * The request to be executed against the service instance selected by the
	 * LoadBalancer.
	 */
	private final Object clientRequest;

	public DefaultRequestContext() {
		clientRequest = null;
	}

	public DefaultRequestContext(Object clientRequest) {
		this.clientRequest = clientRequest;
	}

	public DefaultRequestContext(Object clientRequest, String hint) {
		super(hint);
		this.clientRequest = clientRequest;
	}

	public Object getClientRequest() {
		return clientRequest;
	}

	@Override
	public String toString() {
		ToStringCreator to = new ToStringCreator(this);
		to.append("clientRequest", clientRequest);
		return to.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof DefaultRequestContext)) {
			return false;
		}
		DefaultRequestContext that = (DefaultRequestContext) o;
		return Objects.equals(clientRequest, that.clientRequest);
	}

	@Override
	public int hashCode() {
		return Objects.hash(clientRequest);
	}

}
