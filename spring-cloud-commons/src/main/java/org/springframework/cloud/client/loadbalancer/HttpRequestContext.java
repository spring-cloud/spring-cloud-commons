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

import org.springframework.http.HttpRequest;

/**
 * @author Olga Maciaszek-Sharma
 */
public class HttpRequestContext extends DefaultRequestContext {

	public HttpRequestContext(HttpRequest httpRequest) {
		this(httpRequest, "default");
	}

	public HttpRequestContext(HttpRequest httpRequest, String hint) {
		super(httpRequest, hint);
	}

	public HttpRequest getClientRequest() {
		return (HttpRequest) super.getClientRequest();
	}

}
