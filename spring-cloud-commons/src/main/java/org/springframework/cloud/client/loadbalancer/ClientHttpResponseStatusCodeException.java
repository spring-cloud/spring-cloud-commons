/*
 * Copyright 2016-2018 the original author or authors.
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
package org.springframework.cloud.client.loadbalancer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.AbstractClientHttpResponse;
import org.springframework.http.client.ClientHttpResponse;

/**
 * {@link RetryableStatusCodeException} that captures a {@link ClientHttpResponse}
 * @author Ryan Baxter
 */
public class ClientHttpResponseStatusCodeException extends RetryableStatusCodeException {

	private ClientHttpResponseWrapper response;

	/**
	 * Constructor
	 * @param serviceId The service id
	 * @param response The response object
	 * @throws IOException Thrown if the {@link ClientHttpResponse} response code cant be retrieved
	 */
	public ClientHttpResponseStatusCodeException(String serviceId, ClientHttpResponse response, byte[] body) throws IOException {
		super(serviceId, response.getRawStatusCode(), response, null);
		this.response = new ClientHttpResponseWrapper(response, body);
	}

	@Override
	public ClientHttpResponse getResponse() {
		return response;
	}

	static class ClientHttpResponseWrapper extends AbstractClientHttpResponse {

		private ClientHttpResponse response;
		private byte[] body;

		public ClientHttpResponseWrapper(ClientHttpResponse response, byte[] body) {
			this.response = response;
			this.body = body;
		}

		@Override
		public int getRawStatusCode() throws IOException {
			return response.getRawStatusCode();
		}

		@Override
		public String getStatusText() throws IOException {
			return response.getStatusText();
		}

		@Override
		public void close() {
			response.close();
		}

		@Override
		public InputStream getBody() throws IOException {
			return new ByteArrayInputStream(body);
		}

		@Override
		public HttpHeaders getHeaders() {
			return response.getHeaders();
		}
	}
}
