/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.cloud.loadbalancer.blocking;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.mock.http.client.MockClientHttpRequest;

/**
 * Tests for LoadBalancerX-Transformer.
 *
 * @author Gandhimathi Velusamy
 */

public class XForwarderHeadersTransformerTests {

	private final LoadBalancerProperties.Xforwarded xforwarded = new LoadBalancerProperties().getXforwarded();

	XForwarderHeadersTransformer transformer = new XForwarderHeadersTransformer(xforwarded);

	ServiceInstance serviceInstance = new DefaultServiceInstance("test-01", "test", "host", 8080, false);

	@Test
	public void shouldAppendXforwardHeaderIfEnabledXforward() throws NullPointerException, URISyntaxException {

		if (xforwarded.isEnabledXforwarded()) {
			MockClientHttpRequest request = new MockClientHttpRequest();
			request.setMethod(HttpMethod.GET);
			request.setURI(new URI("http://google.com/"));
			HttpRequest newRequest = transformer.transformRequest(request, serviceInstance);
			String xForwardHost = newRequest.getHeaders().getFirst("X-Forwarded-Host");
			assert xForwardHost != null;
			assert (xForwardHost.equals("google.com"));
			String xForwardProto = newRequest.getHeaders().getFirst("X-Forwarded-Proto");
			assert (xForwardProto != null);
			assert (xForwardProto.equals("http"));
		}
	}

}
