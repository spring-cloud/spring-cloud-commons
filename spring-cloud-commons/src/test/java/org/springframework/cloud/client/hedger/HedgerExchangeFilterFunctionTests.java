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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Kevin Binswanger
 */
@RunWith(MockitoJUnitRunner.class)
public class HedgerExchangeFilterFunctionTests {
	private HedgerPolicy hedgerPolicy;
	private ClientRequest clientRequest;
	private ExchangeFunction exchangeFunction;
	private HedgerListener metricsReporter;
	private HedgerPolicyFactory hedgerPolicyFactory;

	@Before
	public void setup() {
		this.hedgerPolicy = mock(HedgerPolicy.class);
		this.clientRequest = mock(ClientRequest.class);
		this.exchangeFunction = mock(ExchangeFunction.class);
		this.metricsReporter = mock(HedgerListener.class);
		this.hedgerPolicyFactory = new HedgerPolicyFactory() {
			@Override
			public HedgerPolicy getHedgingPolicy(ClientRequest request) {
				return hedgerPolicy;
			}

			@Override
			public HedgerListener[] getHedgingListeners(ClientRequest request) {
				return new HedgerListener[] {metricsReporter};
			}
		};
	}

	@After
	public void teardown() {
		this.hedgerPolicy = null;
		this.clientRequest = null;
		this.exchangeFunction = null;
		this.metricsReporter = null;
		this.hedgerPolicyFactory = null;
	}

	@Test
	public void noHedgesShouldNotHedge() {
		when(hedgerPolicy.getMaximumHedgedRequests(clientRequest)).thenReturn(0);
		when(hedgerPolicy.getDelayBeforeHedging(clientRequest)).thenReturn(Duration.of(100, ChronoUnit.SECONDS));
		when(exchangeFunction.exchange(clientRequest)).thenReturn(Mono.empty()); // to make post-success reporting work

		HedgerExchangeFilterFunction filter = new HedgerExchangeFilterFunction(hedgerPolicyFactory);
		filter.filter(clientRequest, exchangeFunction);

		verify(exchangeFunction, times(1)).exchange(clientRequest);
	}

	@Test
	public void negativeDelayShouldNotHedge() {
		when(hedgerPolicy.getDelayBeforeHedging(clientRequest)).thenReturn(Duration.of(-1, ChronoUnit.SECONDS));
		when(exchangeFunction.exchange(clientRequest)).thenReturn(Mono.empty()); // to make post-success reporting work

		HedgerExchangeFilterFunction filter = new HedgerExchangeFilterFunction(hedgerPolicyFactory);
		filter.filter(clientRequest, exchangeFunction);

		verify(exchangeFunction, times(1)).exchange(clientRequest);
	}

	@Test
	public void correctlyReportMetricsNoHedging() {
		when(hedgerPolicy.getMaximumHedgedRequests(clientRequest)).thenReturn(1);
		when(hedgerPolicy.getDelayBeforeHedging(clientRequest)).thenReturn(Duration.of(100, ChronoUnit.SECONDS));

		ClientResponse response = mock(ClientResponse.class);
		when(exchangeFunction.exchange(clientRequest)).thenReturn(Mono.just(response));

		HedgerExchangeFilterFunction filter = new HedgerExchangeFilterFunction(hedgerPolicyFactory);
		Mono<ClientResponse> actual = filter.filter(clientRequest, exchangeFunction);

		then(actual.block()).isEqualTo(response);
		verify(metricsReporter).record(eq(clientRequest), eq(response), anyLong(), eq(0));
	}

	@Test
	public void hedgeOnceButOriginalReturnsFirst() {
		when(hedgerPolicy.getMaximumHedgedRequests(clientRequest)).thenReturn(1);
		when(hedgerPolicy.getDelayBeforeHedging(clientRequest)).thenReturn(Duration.of(100, ChronoUnit.MILLIS));

		ClientResponse response = mock(ClientResponse.class);
		StepVerifier.withVirtualTime(() -> {
			AtomicInteger count = new AtomicInteger(0);
			Mono<ClientResponse> first = Mono.just(response).delayElement(Duration.ofMillis(200));
			Mono<ClientResponse> second = Mono.never();
			when(exchangeFunction.exchange(clientRequest)).then(invocation -> count.getAndIncrement() == 0 ? first : second);

			HedgerExchangeFilterFunction filter = new HedgerExchangeFilterFunction(hedgerPolicyFactory);
			return filter.filter(clientRequest, exchangeFunction);
		})
				.expectSubscription()
				.expectNoEvent(Duration.of(200, ChronoUnit.MILLIS))
				.expectNext(response)
				.verifyComplete();

		verify(metricsReporter).record(clientRequest, response, 200, 0);
	}

	@Test
	public void hedgeOnceOriginalNeverReturns() {
		when(hedgerPolicy.getMaximumHedgedRequests(clientRequest)).thenReturn(1);
		when(hedgerPolicy.getDelayBeforeHedging(clientRequest)).thenReturn(Duration.ofMillis(100));

		ClientResponse response = mock(ClientResponse.class);
		StepVerifier.withVirtualTime(() -> {

			AtomicInteger count = new AtomicInteger(0);
			Mono<ClientResponse> first = Mono.never();
			Mono<ClientResponse> second = Mono.just(response).delayElement(Duration.ofMillis(150));
			when(exchangeFunction.exchange(clientRequest)).then(invocation -> count.getAndIncrement() == 0 ? first : second);

			HedgerExchangeFilterFunction filter = new HedgerExchangeFilterFunction(hedgerPolicyFactory);
			return filter.filter(clientRequest, exchangeFunction);
		})
				.expectSubscription()
				.expectNoEvent(Duration.ofMillis(250))
				.expectNext(response)
				.verifyComplete();

		verify(metricsReporter).record(clientRequest, response, 150, 1);
	}

	@Test
	public void firstRequestSucceedsNeverHedge() {
		when(hedgerPolicy.getMaximumHedgedRequests(clientRequest)).thenReturn(2);
		when(hedgerPolicy.getDelayBeforeHedging(clientRequest)).thenReturn(Duration.ofMillis(100));

		ClientResponse response = mock(ClientResponse.class);
		StepVerifier.withVirtualTime(() -> {
			when(exchangeFunction.exchange(clientRequest)).thenReturn(Mono.just(response).delayElement(Duration.ofMillis(50)));

			HedgerExchangeFilterFunction filter = new HedgerExchangeFilterFunction(hedgerPolicyFactory);
			return filter.filter(clientRequest, exchangeFunction);
		})
				.expectSubscription()
				.expectNoEvent(Duration.ofMillis(50))
				.expectNext(response)
				.verifyComplete();

		verify(metricsReporter).record(clientRequest, response, 50, 0);
		verify(exchangeFunction, times(1)).exchange(clientRequest);
	}

	@Test
	public void secondRequestSucceedsDontMakeThird() {
		when(hedgerPolicy.getMaximumHedgedRequests(clientRequest)).thenReturn(5);
		when(hedgerPolicy.getDelayBeforeHedging(clientRequest)).thenReturn(Duration.ofMillis(100));

		ClientResponse response = mock(ClientResponse.class);
		StepVerifier.withVirtualTime(() -> {
			AtomicInteger count = new AtomicInteger(0);
			when(exchangeFunction.exchange(clientRequest)).then(invocation -> count.getAndIncrement() == 1 ? Mono.just(response).delayElement(Duration.ofMillis(50)) : Mono.never());

			HedgerExchangeFilterFunction filter = new HedgerExchangeFilterFunction(hedgerPolicyFactory);
			return filter.filter(clientRequest, exchangeFunction);
		})
				.expectSubscription()
				.expectNoEvent(Duration.ofMillis(150))
				.expectNext(response)
				.verifyComplete();

		verify(metricsReporter).record(clientRequest, response, 50, 1);
		verify(exchangeFunction, times(2)).exchange(clientRequest);
	}
}
