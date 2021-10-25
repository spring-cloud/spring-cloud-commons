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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;

import static java.net.URI.create;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for XForwadededHeadersTransformer.
 *
 * @author Gandhimathi Velusamy
 */

public class XForwardedHeadersTransformerTests {

	private final LoadBalancerProperties.XForwarded xForwarded = new LoadBalancerProperties().getxForwarded();

	private final ServiceInstance serviceInstance = mock(DefaultServiceInstance.class);

	private final HttpRequest request = mock(HttpRequest.class);

	@BeforeEach
	void setUp() {
		when(serviceInstance.getInstanceId()).thenReturn("test1");
		when(request.getMethod()).thenReturn(HttpMethod.GET);
		when(request.getURI()).thenReturn(create("https://google.com"));
		when(request.getHeaders()).thenReturn(new HttpHeaders());
	}

	@Test
	void shouldAppendXforwardHeaderIfEnabledXforward() throws NullPointerException {
		XForwardedHeadersTransformer transformer = new XForwardedHeadersTransformer(xForwarded);
		xForwarded.setEnabledXforwarded(true);
		HttpRequest newRequest = transformer.transformRequest(request, serviceInstance);

		assertThat(newRequest.getHeaders().containsKey("X-Forwarded-Host")).isTrue();
		assertThat(newRequest.getHeaders().getFirst("X-Forwarded-Host")).isEqualTo("google.com");
		assertThat(newRequest.getHeaders().containsKey("X-Forwarded-Proto")).isTrue();
		assertThat(newRequest.getHeaders().getFirst("X-Forwarded-Proto")).isEqualTo("https");
	}

	@Test
	void shouldNotAppendXforwardedHeaderIfDefault() {

		XForwardedHeadersTransformer transformer = new XForwardedHeadersTransformer(xForwarded);
		HttpRequest newRequest = transformer.transformRequest(request, serviceInstance);
		assertThat(newRequest.getHeaders().containsKey("X-Forwarded-Host")).isFalse();
		assertThat(newRequest.getHeaders().containsKey("X-Forwarded-Proto")).isFalse();
	}

}
