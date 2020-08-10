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

import java.util.function.Predicate;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MockClock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import org.springframework.web.reactive.function.client.ClientRequest;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Kevin Binswanger
 */
@RunWith(MockitoJUnitRunner.class)
public class LatencyPercentileHedgerPolicyTests {
	private Predicate<ClientRequest> predicate;
	private Clock clock;

	@Before
	public void setup() {
		//noinspection unchecked
		this.predicate = mock(Predicate.class);
		this.clock = new MockClock();
	}

	@After
	public void teardown() {
		this.predicate = null;
		this.clock = null;
	}

	@Test
	public void testNumberOfHedgedRequests() {
		int numberOfHedgedRequests = 23;
		LatencyPercentileHedgerPolicy client = new LatencyPercentileHedgerPolicy(
				predicate, 23, 0.0, clock
		);

		then(client.getMaximumHedgedRequests(mock(ClientRequest.class))).isEqualTo(numberOfHedgedRequests);
	}

	@Test
	public void shouldHedge() {
		ClientRequest request = mock(ClientRequest.class);
		when(predicate.test(request)).thenReturn(true);

		LatencyPercentileHedgerPolicy client = new LatencyPercentileHedgerPolicy(
				predicate, 1, 0.0, clock
		);

		then(client.getMaximumHedgedRequests(request)).isEqualTo(1);
	}

	@Test
	public void shouldNotHedge() {
		ClientRequest request = mock(ClientRequest.class);
		when(predicate.test(request)).thenReturn(false);

		LatencyPercentileHedgerPolicy client = new LatencyPercentileHedgerPolicy(
				predicate, 1, 0.0, clock
		);

		then(client.getMaximumHedgedRequests(request)).isEqualTo(0);
	}
}
