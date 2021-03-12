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
 * A default implementation of {@link Request}.
 *
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
public class DefaultRequest<T> implements Request<T> {

	private T context;

	public DefaultRequest() {
		new DefaultRequestContext();
	}

	public DefaultRequest(T context) {
		this.context = context;
	}

	@Override
	public T getContext() {
		return context;
	}

	public void setContext(T context) {
		this.context = context;
	}

	@Override
	public String toString() {
		ToStringCreator to = new ToStringCreator(this);
		to.append("context", context);
		return to.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof DefaultRequest)) {
			return false;
		}
		DefaultRequest<?> that = (DefaultRequest<?>) o;
		return Objects.equals(context, that.context);
	}

	@Override
	public int hashCode() {
		return Objects.hash(context);
	}

}
