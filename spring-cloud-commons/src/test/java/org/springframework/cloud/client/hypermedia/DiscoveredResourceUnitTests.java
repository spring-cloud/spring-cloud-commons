/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.cloud.client.hypermedia;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.client.Traverson;
import org.springframework.hateoas.client.Traverson.TraversalBuilder;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class DiscoveredResourceUnitTests {

	@Mock ServiceInstanceProvider provider;
	@Mock TraversalDefinition traversal;
	@Mock TraversalBuilder builder;
	@Mock RestOperations operations;

	DiscoveredResource resource;

	@Before
	public void setUp() {
		when(traversal.buildTraversal(Matchers.any(Traverson.class))).thenReturn(builder);

		this.resource = new DiscoveredResource(provider, traversal);
		this.resource.setRestOperations(operations);
	}

	@Test
	public void isUndiscoveredByDefault() {
		assertThat(resource.getLink(), is(nullValue()));
	}

	@Test
	public void verificationTriggersDiscovery() {

		Link link = new Link("target", "rel");

		when(provider.getServiceInstance()).thenReturn(new DefaultServiceInstance("service", "localhost", 8080, false));
		when(builder.asTemplatedLink()).thenReturn(link);

		resource.verifyOrDiscover();

		assertThat(resource.getLink(), is(link));
		verify(provider, times(1)).getServiceInstance();
		verify(traversal, times(1)).buildTraversal(Matchers.any(Traverson.class));
	}

	@Test
	public void triggersVerificationOnSubsequentCall() {

		verificationTriggersDiscovery();

		resource.verifyOrDiscover();

		assertThat(resource.getLink(), is(notNullValue()));
		verify(operations, times(1)).headForHeaders(anyString());
	}

	@Test
	public void resetsLinkOnFailedVerification() {

		verificationTriggersDiscovery();

		doThrow(RestClientException.class).when(operations).headForHeaders(anyString());
		resource.verifyOrDiscover();

		assertThat(resource.getLink(), is(nullValue()));
	}

	@Test
	public void failedDiscoveryTraversalCausesLinkToStayNull() {

		doThrow(RuntimeException.class).when(provider).getServiceInstance();

		resource.verifyOrDiscover();

		assertThat(resource.getLink(), is(nullValue()));
	}
}
