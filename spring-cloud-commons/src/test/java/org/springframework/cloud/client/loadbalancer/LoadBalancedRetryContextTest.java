package org.springframework.cloud.client.loadbalancer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpRequest;
import org.springframework.retry.RetryContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
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
		assertThat(lbContext.getRequest(), is(this.request));
	}

	@Test
	public void setRequest() throws Exception {
		LoadBalancedRetryContext lbContext = new LoadBalancedRetryContext(this.context,
				this.request);
		HttpRequest newRequest = mock(HttpRequest.class);
		lbContext.setRequest(newRequest);
		assertThat(lbContext.getRequest(), is(newRequest));
	}

	@Test
	public void getServiceInstance() throws Exception {
		LoadBalancedRetryContext lbContext = new LoadBalancedRetryContext(this.context,
				this.request);
		ServiceInstance serviceInstance = mock(ServiceInstance.class);
		lbContext.setServiceInstance(serviceInstance);
		assertThat(lbContext.getServiceInstance(), is(serviceInstance));
	}

}
