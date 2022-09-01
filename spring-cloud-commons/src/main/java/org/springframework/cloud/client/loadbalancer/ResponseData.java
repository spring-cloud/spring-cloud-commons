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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.core.style.ToStringCreator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseCookie;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;

/**
 * Represents the data of the request that can be safely read (without passing request
 * reactive stream values).
 *
 * @author Olga Maciaszek-Sharma
 * @since 3.0.0
 */
public class ResponseData {

	private final HttpStatusCode httpStatus;

	private final HttpHeaders headers;

	private final MultiValueMap<String, ResponseCookie> cookies;

	private final RequestData requestData;

	public ResponseData(HttpStatusCode httpStatus, HttpHeaders headers, MultiValueMap<String, ResponseCookie> cookies,
			RequestData requestData) {
		this.httpStatus = httpStatus;
		this.headers = headers;
		this.cookies = cookies;
		this.requestData = requestData;
	}

	public ResponseData(ClientResponse response, RequestData requestData) {
		this(response.statusCode(), response.headers().asHttpHeaders(), response.cookies(), requestData);
	}

	public ResponseData(ServerHttpResponse response, RequestData requestData) {
		this(response.getStatusCode(), response.getHeaders(), response.getCookies(), requestData);
	}

	public ResponseData(ClientHttpResponse clientHttpResponse, RequestData requestData) throws IOException {
		this(clientHttpResponse.getStatusCode(), clientHttpResponse.getHeaders(),
				buildCookiesFromHeaders(clientHttpResponse.getHeaders()), requestData);
	}

	public HttpStatusCode getHttpStatus() {
		return httpStatus;
	}

	public HttpHeaders getHeaders() {
		return headers;
	}

	public MultiValueMap<String, ResponseCookie> getCookies() {
		return cookies;
	}

	public RequestData getRequestData() {
		return requestData;
	}

	@Override
	public String toString() {
		ToStringCreator to = new ToStringCreator(this);
		to.append("httpStatus", httpStatus);
		return to.toString();
	}

	static MultiValueMap<String, ResponseCookie> buildCookiesFromHeaders(HttpHeaders headers) {
		LinkedMultiValueMap<String, ResponseCookie> newCookies = new LinkedMultiValueMap<>();
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
				newCookies.put(splitCookie[0],
						Collections.singletonList(ResponseCookie.from(splitCookie[0], splitCookie[1]).build()));
			});
		}
		return newCookies;
	}

	@Override
	public int hashCode() {
		return Objects.hash(httpStatus, headers, cookies, requestData);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ResponseData that)) {
			return false;
		}
		return httpStatus == that.httpStatus && Objects.equals(headers, that.headers)
				&& Objects.equals(cookies, that.cookies) && Objects.equals(requestData, that.requestData);
	}

}
