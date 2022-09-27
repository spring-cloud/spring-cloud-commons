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

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.core.style.ToStringCreator;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientRequest;

/**
 * Represents the data of the request that can be safely read (without passing request
 * reactive stream values).
 *
 * @author Olga Maciaszek-Sharma
 * @since 3.0.0
 */
public class RequestData {

	private final HttpMethod httpMethod;

	private final URI url;

	private final HttpHeaders headers;

	private final MultiValueMap<String, String> cookies;

	private final Map<String, Object> attributes;

	public RequestData(HttpMethod httpMethod, URI url, HttpHeaders headers, MultiValueMap<String, String> cookies,
			Map<String, Object> attributes) {
		this.httpMethod = httpMethod;
		this.url = url;
		this.headers = headers;
		this.cookies = cookies;
		this.attributes = attributes;
	}

	public RequestData(ClientRequest request) {
		this(request.method(), request.url(), request.headers(), request.cookies(), request.attributes());
	}

	public RequestData(HttpRequest request) {
		this(request.getMethod(), request.getURI(), request.getHeaders(), buildCookiesFromHeaders(request.getHeaders()),
				new HashMap<>());
	}

	public RequestData(ServerHttpRequest request) {
		this(request.getMethod(), request.getURI(), request.getHeaders(), buildCookies(request.getCookies()),
				new HashMap<>());
	}

	public RequestData(ServerHttpRequest request, Map<String, Object> attributes) {
		this(request.getMethod(), request.getURI(), request.getHeaders(), buildCookies(request.getCookies()),
				attributes);
	}

	private static MultiValueMap<String, String> buildCookies(MultiValueMap<String, HttpCookie> cookies) {
		HttpHeaders newCookies = new HttpHeaders();
		if (cookies != null) {
			cookies.forEach((key, value) -> value
					.forEach(cookie -> newCookies.put(cookie.getName(), Collections.singletonList(cookie.getValue()))));
		}
		return newCookies;
	}

	private static MultiValueMap<String, String> buildCookiesFromHeaders(HttpHeaders headers) {
		HttpHeaders newCookies = new HttpHeaders();
		if (headers == null) {
			return newCookies;
		}
		List<String> cookiesFromHeaders = headers.get(HttpHeaders.COOKIE);
		if (cookiesFromHeaders != null) {
			cookiesFromHeaders.forEach(cookie -> {
				String[] splitCookie = cookie.split("=");
				if (splitCookie.length < 2) {
					return;
				}
				newCookies.put(splitCookie[0], Collections.singletonList(splitCookie[1]));
			});
		}
		return newCookies;
	}

	public HttpMethod getHttpMethod() {
		return httpMethod;
	}

	public URI getUrl() {
		return url;
	}

	public HttpHeaders getHeaders() {
		return headers;
	}

	public MultiValueMap<String, String> getCookies() {
		return cookies;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	@Override
	public String toString() {
		ToStringCreator to = new ToStringCreator(this);
		to.append("httpMethod", httpMethod);
		to.append("url", url);
		to.append("headers", headers);
		to.append("cookies", cookies);
		return to.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof RequestData)) {
			return false;
		}
		RequestData that = (RequestData) o;
		return httpMethod == that.httpMethod && Objects.equals(url, that.url) && Objects.equals(headers, that.headers)
				&& Objects.equals(cookies, that.cookies) && Objects.equals(attributes, that.attributes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(httpMethod, url, headers, cookies, attributes);
	}

}
