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
import org.springframework.web.reactive.function.client.ClientResponse;

/**
 * Hedging could create multiple requests to a given service where an application previously made one. This provides
 * a hedging-aware mechanism to do any special handling necessary. Example uses:
 * <ul>
 *     <li>Track the latency of each individual request instead of the total batch<./li>
 *     <li>Tag a metric with the request number.</li>
 *     <li>Track the number of hedged requests made.</li>
 * </ul>
 * @author Csaba Kos
 * @author Kevin Binswanger
 */
@FunctionalInterface
public interface HedgerListener {
	/**
	 * Invoked by {@link HedgerExchangeFilterFunction} on every individual attempt.
	 * @param request The HTTP request.
	 * @param response The eventual HTTP response.
	 * @param elapsedMillis The number of milliseconds elapsed just for this attempt (ignores time spent waiting on
	 *                      previous attempts).
	 * @param requestNumber Which attempt this is. The original will be 0, and the first hedged request will be 1.
	 */
	void record(ClientRequest request,
				ClientResponse response,
				long elapsedMillis,
				int requestNumber);
}
