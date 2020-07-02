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

package org.springframework.cloud.client.loadbalancer.reactive;

/**
 * A default implementation of {@link Request}.
 *
 * @deprecated in favour of
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
@Deprecated
public class DefaultRequest<T>
		extends org.springframework.cloud.client.loadbalancer.DefaultRequest<T>
		implements Request<T> {

	public DefaultRequest() {
		new DefaultRequestContext();
	}

	public DefaultRequest(T context) {
		super(context);
	}

}
