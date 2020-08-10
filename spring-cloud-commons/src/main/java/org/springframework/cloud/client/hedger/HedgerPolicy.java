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

import java.time.Duration;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Controls the parameters for a {@link WebClient.Builder} annotated with {@link Hedged}.
 * @author Kevin Binswanger
 */
public interface HedgerPolicy {
	/**
	 * The maximum number of requests that could be made if the original does not succeed in time.
	 * @param request The HTTP request to be made.
	 * @return How many extra requests can be made. 0 or less will do no hedging.
	 */
	int getMaximumHedgedRequests(ClientRequest request);

	/**
	 * How long to wait after each request to execute the next one. When this time is reached, a single extra request
	 * will be made. Then that duration has to pass *again* before making the second request, and so on.
	 * @param request The HTTP request to be made.
	 * @return How long to wait after each request.
	 */
	Duration getDelayBeforeHedging(ClientRequest request);
}
