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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * @author Ryan Baxter
 */
@RunWith(MockitoJUnitRunner.class)
public class ClientHttpResponseStatusCodeExceptionTest {

	@Test
	public void testCreation() throws Exception {
		MyClientHttpResponse response = new MyClientHttpResponse();
		assertFalse(response.isClosed());
		ClientHttpResponseStatusCodeException exp = new ClientHttpResponseStatusCodeException("service",
				response, response.getStatusText().getBytes());
		ClientHttpResponse expResponse = exp.getResponse();
		assertEquals(response.getRawStatusCode(), expResponse.getRawStatusCode());
		assertEquals(response.getStatusText(), expResponse.getStatusText());
		assertEquals(response.getHeaders(), expResponse.getHeaders());
		assertEquals(response.getStatusText(), new String(StreamUtils.copyToByteArray(expResponse.getBody())));
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
			return closed;
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