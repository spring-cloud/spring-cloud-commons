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

import org.junit.jupiter.api.Test;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClientProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerPropertiesFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.mock.http.client.MockClientHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LoadBalancerServiceInstanceCookieTransformer}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Andrii Bohutskyi
 */
class LoadBalancerServiceInstanceCookieTransformerPerServiceTests {

	LoadBalancerClientProperties serviceProperties = new LoadBalancerClientProperties();

	LoadBalancerPropertiesFactory propertiesFactory = new LoadBalancerPropertiesFactory(new LoadBalancerProperties(),
			serviceProperties, true);

	LoadBalancerServiceInstanceCookieTransformer transformer = new LoadBalancerServiceInstanceCookieTransformer(
			new LoadBalancerProperties().getStickySession(), propertiesFactory);

	ServiceInstance serviceInstance = new DefaultServiceInstance("test-01", "test", "host", 8080, false);

	HttpRequest request = new MockClientHttpRequest();

	@Test
	void shouldAddServiceInstanceCookieHeader() {
		HttpRequest newRequest = transformer.transformRequest(request, serviceInstance);

		assertThat(newRequest.getHeaders().get(HttpHeaders.COOKIE)).hasSize(1);
		assertThat(newRequest.getHeaders().get(HttpHeaders.COOKIE)).containsExactly("sc-lb-instance-id=test-01");
	}

	@Test
	void shouldAppendServiceInstanceCookieHeaderIfCookiesPresent() {
		request.getHeaders().add(HttpHeaders.COOKIE, "testCookieName=testCookieValue");

		HttpRequest newRequest = transformer.transformRequest(request, serviceInstance);

		assertThat(newRequest.getHeaders().get(HttpHeaders.COOKIE)).containsExactly("testCookieName=testCookieValue",
				"sc-lb-instance-id=test-01");
	}

	@Test
	void shouldReturnPassedRequestWhenNoServiceInstance() {
		HttpRequest newRequest = transformer.transformRequest(request, null);

		assertThat(newRequest.getHeaders()).doesNotContainKey(HttpHeaders.COOKIE);
	}

	@Test
	void shouldReturnPassedRequestWhenNullServiceInstanceCookieName() {
		serviceProperties.getClient().put("test", new LoadBalancerProperties());
		serviceProperties.getClient().get("test").getStickySession().setInstanceIdCookieName(null);

		HttpRequest newRequest = transformer.transformRequest(request, serviceInstance);

		assertThat(newRequest.getHeaders()).doesNotContainKey(HttpHeaders.COOKIE);
	}

	@Test
	void shouldReturnPassedRequestWhenEmptyServiceInstanceCookieName() {
		serviceProperties.getClient().put("test", new LoadBalancerProperties());
		serviceProperties.getClient().get("test").getStickySession().setInstanceIdCookieName("");

		HttpRequest newRequest = transformer.transformRequest(request, serviceInstance);

		assertThat(newRequest.getHeaders()).doesNotContainKey(HttpHeaders.COOKIE);

	}

}
