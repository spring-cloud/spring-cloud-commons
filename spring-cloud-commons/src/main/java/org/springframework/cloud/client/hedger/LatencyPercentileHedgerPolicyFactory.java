/*
 * Copyright 2013-2020 the original author or authors.
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

import org.springframework.web.reactive.function.client.ClientRequest;

/**
 * A convenience {@link HedgerPolicyFactory} to hedge when request times are above a certain percentile. Applies the
 * same {@link LatencyPercentileHedgerPolicy} to all requests.
 * @author Kevin Binswanger
 */
public class LatencyPercentileHedgerPolicyFactory implements HedgerPolicyFactory {
	private final LatencyPercentileHedgerPolicy policy;
	private final HedgerListener[] listeners;

	public LatencyPercentileHedgerPolicyFactory(
			LatencyPercentileHedgerPolicy policy,
			HedgerListener... listeners
	) {
		this.policy = policy;
		this.listeners = concatenateListenersArray(policy, listeners);
	}

	@Override
	public HedgerPolicy getHedgingPolicy(ClientRequest request) {
		return policy;
	}

	@Override
	public HedgerListener[] getHedgingListeners(ClientRequest request) {
		return listeners;
	}

	private static HedgerListener[] concatenateListenersArray(
			LatencyPercentileHedgerPolicy policy,
			HedgerListener... extraListeners
	) {
		HedgerListener[] listeners = new HedgerListener[extraListeners.length + 1];
		listeners[0] = policy;
		System.arraycopy(extraListeners, 0, listeners, 1, extraListeners.length);
		return listeners;
	}
}
