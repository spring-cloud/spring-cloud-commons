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

package org.springframework.cloud.client.hedger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.web.reactive.function.client.ClientRequest;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;

/**
 * @author Kevin Binswanger
 */
public class LatencyPercentileHedgerPolicyFactoryTests {
	private ClientRequest request;
	private LatencyPercentileHedgerPolicy policy;

	@Before
	public void setup() {
		request = mock(ClientRequest.class);
		policy = mock(LatencyPercentileHedgerPolicy.class);
	}

	@After
	public void teardown() {
		request = null;
		policy = null;
	}

	@Test
	public void testReturnsPolicy() {
		LatencyPercentileHedgerPolicyFactory factory = new LatencyPercentileHedgerPolicyFactory(policy);
		then(factory.getHedgingPolicy(request)).isEqualTo(policy);
	}

	@Test
	public void testGetListenersNoExtras() {
		LatencyPercentileHedgerPolicyFactory factory = new LatencyPercentileHedgerPolicyFactory(policy);
		HedgerListener[] listeners = factory.getHedgingListeners(request);
		then(listeners.length).isEqualTo(1);
		then(listeners[0]).isEqualTo(policy);
	}

	@Test
	public void testGetListenersWithExtras() {
		HedgerListener listenerA = mock(HedgerListener.class);
		HedgerListener listenerB = mock(HedgerListener.class);

		LatencyPercentileHedgerPolicyFactory factory = new LatencyPercentileHedgerPolicyFactory(policy, listenerA, listenerB);
		HedgerListener[] listeners = factory.getHedgingListeners(request);
		then(listeners.length).isEqualTo(3);
		then(listeners[0]).isEqualTo(policy);
		then(listeners[1]).isEqualTo(listenerA);
		then(listeners[2]).isEqualTo(listenerB);
	}
}
