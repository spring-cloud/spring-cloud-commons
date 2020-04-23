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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Oliver Gierke
 * @author Tim Ysewyn
 */
@RunWith(MockitoJUnitRunner.class)
public class DiscoveredResourceUnitTests {

	@Mock
	ServiceInstanceProvider provider;

	@Mock
	TraversalDefinition traversal;

	@Mock
	Traverson.TraversalBuilder builder;

	@Mock
	RestOperations operations;

	DiscoveredResource resource;

	@Before
	public void setUp() {
		when(this.traversal.buildTraversal(Matchers.any(Traverson.class)))
				.thenReturn(this.builder);

		this.resource = new DiscoveredResource(this.provider, this.traversal);
		this.resource.setRestOperations(this.operations);
	}

	@Test
	public void isUndiscoveredByDefault() {
		then(this.resource.getLink()).isNull();
	}

	@Test
	public void verificationTriggersDiscovery() {

		Link link = new Link("target", "rel");

		when(this.provider.getServiceInstance()).thenReturn(new DefaultServiceInstance(
				"instance", "service", "localhost", 8080, false));
		when(this.builder.asTemplatedLink()).thenReturn(link);

		this.resource.verifyOrDiscover();

		then(this.resource.getLink()).isEqualTo(link);
		verify(this.provider, times(1)).getServiceInstance();
		verify(this.traversal, times(1)).buildTraversal(Matchers.any(Traverson.class));
	}

	@Test
	public void triggersVerificationOnSubsequentCall() {

		verificationTriggersDiscovery();

		this.resource.verifyOrDiscover();

		then(this.resource.getLink()).isNotNull();
		verify(this.operations, times(1)).headForHeaders(anyString());
	}

	@Test
	public void resetsLinkOnFailedVerification() {

		verificationTriggersDiscovery();

		doThrow(RestClientException.class).when(this.operations)
				.headForHeaders(anyString());
		this.resource.verifyOrDiscover();

		then(this.resource.getLink()).isNull();
	}

	@Test
	public void failedDiscoveryTraversalCausesLinkToStayNull() {

		doThrow(RuntimeException.class).when(this.provider).getServiceInstance();

		this.resource.verifyOrDiscover();

		then(this.resource.getLink()).isNull();
	}

}
