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

package org.springframework.cloud.loadbalancer.core;

import org.junit.Test;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpRequest;
import org.springframework.mock.http.client.MockClientHttpRequest;

/**
 * Tests for LoadBalancerX-Transformer.
 *
 * @author Gandhimathi
 */

public class LoadBalancerXtransformerTests {

	// LoadBalancerProperties.Xforwarded xforwardProperties = new
	// LoadBalancerProperties().getXforwarded();

	LoadBalancerXforwardTransformer transformer = new LoadBalancerXforwardTransformer();

	ServiceInstance serviceInstance = new DefaultServiceInstance("test-01", "test", "host", 8080, false);

	@Test
	public void shouldAppendXforwardHeaderIfenableXforward() {
		HttpRequest request = new MockClientHttpRequest();
		request.getHeaders().add("X-Forward-Host", "hostx");
		request.getHeaders().add("X-Forward-Proto", "https");
		try {
			HttpRequest newRequest = transformer.transformRequest(request, serviceInstance);
			assert (newRequest.getHeaders().getFirst("X-Forward-Host").equals("hostx"));
			assert (newRequest.getHeaders().getFirst("X-Forward-Proto").equals("https"));
		}
		catch (Exception e) {
			e.printStackTrace();
		}

	}

}
