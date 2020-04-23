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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.AbstractClientHttpResponse;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Ryan Baxter
 */
@RunWith(MockitoJUnitRunner.class)
public class ClientHttpResponseStatusCodeExceptionTest {

	@Test
	public void testCreation() throws Exception {
		MyClientHttpResponse response = new MyClientHttpResponse();
		then(response.isClosed()).isFalse();
		ClientHttpResponseStatusCodeException exp = new ClientHttpResponseStatusCodeException(
				"service", response, response.getStatusText().getBytes());
		ClientHttpResponse expResponse = exp.getResponse();
		then(expResponse.getRawStatusCode()).isEqualTo(response.getRawStatusCode());
		then(expResponse.getStatusText()).isEqualTo(response.getStatusText());
		then(expResponse.getHeaders()).isEqualTo(response.getHeaders());
		then(new String(StreamUtils.copyToByteArray(expResponse.getBody())))
				.isEqualTo(response.getStatusText());
	}

	class MyClientHttpResponse extends AbstractClientHttpResponse {

		private boolean closed = false;

		@Override
		public int getRawStatusCode() throws IOException {
			return 200;
		}

		@Override
		public String getStatusText() throws IOException {
			return "foo";
		}

		@Override
		public void close() {
			this.closed = true;
		}

		public boolean isClosed() {
			return this.closed;
		}

		@Override
		public InputStream getBody() throws IOException {
			return new ByteArrayInputStream(getStatusText().getBytes());
		}

		@Override
		public HttpHeaders getHeaders() {
			HttpHeaders headers = new HttpHeaders();
			headers.add("foo", "bar");
			return headers;
		}

	}

}
