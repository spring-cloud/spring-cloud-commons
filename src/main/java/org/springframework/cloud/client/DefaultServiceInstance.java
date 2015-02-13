/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.client;

import java.net.URI;

import lombok.Data;

/**
 * Default implementation of {@link ServiceInstance}.
 *
 * @author Spencer Gibb
 */
@Data
public class DefaultServiceInstance implements ServiceInstance {

	private final String serviceId;

	private final String host;

	private final int port;

	private final boolean secure;

	@Override
	public URI getUri() {
		return getUri(this);
	}

	/**
	 * Create a uri from the given ServiceInstance's host:port
	 * @param instance
	 * @return URI of the form (secure)?https:http + "host:port"
	 */
	public static URI getUri(ServiceInstance instance) {
		String scheme = (instance.isSecure()) ? "https" : "http";
		String uri = String.format("%s://%s:%s", scheme, instance.getHost(),
				instance.getPort());
		return URI.create(uri);
	}
}
