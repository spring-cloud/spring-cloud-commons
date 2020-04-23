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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpRequest;
import org.springframework.retry.RetryContext;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;

/**
 * @author Ryan Baxter
 */
@RunWith(MockitoJUnitRunner.class)
public class LoadBalancedRetryContextTest {

	private RetryContext context;

	private HttpRequest request;

	@Before
	public void setUp() throws Exception {
		this.context = mock(RetryContext.class);
		this.request = mock(HttpRequest.class);
	}

	@After
	public void tearDown() throws Exception {
		this.context = null;
		this.request = null;
	}

	@Test
	public void getRequest() throws Exception {
		LoadBalancedRetryContext lbContext = new LoadBalancedRetryContext(this.context,
				this.request);
		then(lbContext.getRequest()).isEqualTo(this.request);
	}

	@Test
	public void setRequest() throws Exception {
		LoadBalancedRetryContext lbContext = new LoadBalancedRetryContext(this.context,
				this.request);
		HttpRequest newRequest = mock(HttpRequest.class);
		lbContext.setRequest(newRequest);
		then(lbContext.getRequest()).isEqualTo(newRequest);
	}

	@Test
	public void getServiceInstance() throws Exception {
		LoadBalancedRetryContext lbContext = new LoadBalancedRetryContext(this.context,
				this.request);
		ServiceInstance serviceInstance = mock(ServiceInstance.class);
		lbContext.setServiceInstance(serviceInstance);
		then(lbContext.getServiceInstance()).isEqualTo(serviceInstance);
	}

}
