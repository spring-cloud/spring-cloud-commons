/*
 * Copyright 2013-2021 the original author or authors.
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

package org.springframework.cloud.commons.publisher;

import java.time.Duration;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Operators;
import reactor.test.StepVerifier;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

/**
 * @author Tim Ysewyn
 */
public class FluxFirstNonEmptyEmittingTests {

	@Test
	public void arrayNull() {
		assertThatNullPointerException().isThrownBy(() -> CloudFlux.firstNonEmpty((Publisher<Integer>[]) null));
	}

	@Test
	public void iterableNull() {
		assertThatNullPointerException().isThrownBy(() -> CloudFlux.firstNonEmpty((Iterable<Publisher<Integer>>) null));
	}

	@Test
	public void firstWinner() {
		StepVerifier.create(CloudFlux.firstNonEmpty(Flux.range(1, 10), Flux.range(11, 10)))
				.expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).verifyComplete();
	}

	@Test
	public void firstWinnerSecondEmpty() {
		StepVerifier.create(CloudFlux.firstNonEmpty(Flux.range(1, 10), Flux.empty()))
				.expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).verifyComplete();
	}

	@Test
	public void firstWinnerBackpressured() {
		StepVerifier.create(CloudFlux.firstNonEmpty(Flux.range(1, 10), Flux.range(11, 10))).thenRequest(5)
				.expectNext(1, 2, 3, 4, 5).thenCancel().verifyThenAssertThat().hasNotDiscardedElements()
				.hasNotDroppedElements().hasNotDroppedErrors();
	}

	@Test
	public void secondWinner() {
		StepVerifier.create(CloudFlux.firstNonEmpty(Flux.never(), Flux.range(11, 10).log()))
				.expectNext(11, 12, 13, 14, 15, 16, 17, 18, 19, 20).verifyComplete();
	}

	@Test
	public void secondWinnerFirstEmpty() {
		StepVerifier.create(CloudFlux.firstNonEmpty(Flux.empty(), Flux.range(11, 10).log()))
				.expectNext(11, 12, 13, 14, 15, 16, 17, 18, 19, 20).verifyComplete();
	}

	@Test
	public void bothEmpty() {
		StepVerifier.create(CloudFlux.firstNonEmpty(Flux.empty(), Flux.empty())).expectComplete().verifyThenAssertThat()
				.hasNotDiscardedElements().hasNotDroppedElements().hasNotDroppedErrors();
	}

	@Test
	public void neverAndEmpty() {
		StepVerifier.withVirtualTime(() -> CloudFlux.firstNonEmpty(Flux.never(), Flux.empty())).expectSubscription()
				.expectNoEvent(Duration.ofDays(1)).thenCancel().verifyThenAssertThat().hasNotDiscardedElements()
				.hasNotDroppedElements().hasNotDroppedErrors();
	}

	@Test
	public void firstEmitsError() {
		RuntimeException ex = new RuntimeException("forced failure");
		StepVerifier.create(CloudFlux.firstNonEmpty(Flux.<Integer>error(ex), Flux.empty()))
				.expectErrorMessage("forced failure").verifyThenAssertThat().hasNotDiscardedElements()
				.hasNotDroppedElements().hasNotDroppedErrors();
	}

	@Test
	public void secondEmitsError() {
		RuntimeException ex = new RuntimeException("forced failure");
		StepVerifier.create(CloudFlux.firstNonEmpty(Flux.empty(), Flux.<Integer>error(ex)))
				.expectErrorMessage("forced failure").verifyThenAssertThat().hasNotDiscardedElements()
				.hasNotDroppedElements().hasNotDroppedErrors();
	}

	@Test
	public void neverAndSecondEmitsError() {
		RuntimeException ex = new RuntimeException("forced failure");
		StepVerifier.create(CloudFlux.firstNonEmpty(Flux.never(), Flux.<Integer>error(ex)))
				.expectErrorMessage("forced failure").verifyThenAssertThat().hasNotDiscardedElements()
				.hasNotDroppedElements().hasNotDroppedErrors();
	}

	@Test
	public void singleArrayNullSource() {
		StepVerifier.create(CloudFlux.firstNonEmpty((Publisher<Object>) null)).expectError(NullPointerException.class)
				.verify();
	}

	@Test
	public void arrayOneIsNullSource() {
		StepVerifier.create(CloudFlux.firstNonEmpty(Flux.never(), null, Flux.never()))
				.expectError(NullPointerException.class).verify();
	}

	@Test
	public void singleIterableNullSource() {
		StepVerifier.create(CloudFlux.firstNonEmpty(singletonList((Publisher<Object>) null)))
				.expectError(NullPointerException.class).verify();
	}

	@Test
	public void iterableOneIsNullSource() {
		StepVerifier
				.create(CloudFlux.firstNonEmpty(Arrays.asList(Flux.never(), (Publisher<Object>) null, Flux.never())))
				.expectError(NullPointerException.class).verify();
	}

	@Test
	public void scanSubscriber() {
		CoreSubscriber<String> actual = new TestSubscriber<>();
		FluxFirstNonEmptyEmitting.RaceCoordinator<String> parent = new FluxFirstNonEmptyEmitting.RaceCoordinator<>(1);
		FluxFirstNonEmptyEmitting.FirstNonEmptyEmittingSubscriber<String> test = new FluxFirstNonEmptyEmitting.FirstNonEmptyEmittingSubscriber<>(
				actual, parent, 1);
		Subscription sub = Operators.emptySubscription();
		test.onSubscribe(sub);

		assertThat(test.scan(Scannable.Attr.PARENT)).isSameAs(sub);
		assertThat(test.scan(Scannable.Attr.ACTUAL)).isSameAs(actual);
		assertThat(test.scan(Scannable.Attr.CANCELLED)).isFalse();
		parent.cancelled = true;
		assertThat(test.scan(Scannable.Attr.CANCELLED)).isTrue();
	}

	@Test
	public void scanRaceCoordinator() {
		CoreSubscriber<String> actual = new TestSubscriber<>();
		FluxFirstNonEmptyEmitting.RaceCoordinator<String> parent = new FluxFirstNonEmptyEmitting.RaceCoordinator<>(1);
		FluxFirstNonEmptyEmitting.FirstNonEmptyEmittingSubscriber<String> test = new FluxFirstNonEmptyEmitting.FirstNonEmptyEmittingSubscriber<>(
				actual, parent, 1);
		Subscription sub = Operators.emptySubscription();
		test.onSubscribe(sub);

		assertThat(test.scan(Scannable.Attr.PARENT)).isSameAs(sub);
		assertThat(test.scan(Scannable.Attr.ACTUAL)).isSameAs(actual);
		assertThat(parent.scan(Scannable.Attr.CANCELLED)).isFalse();
		parent.cancelled = true;
		assertThat(parent.scan(Scannable.Attr.CANCELLED)).isTrue();
	}

	static class TestSubscriber<T> extends BaseSubscriber<T> implements Scannable {

		@Override
		public Object scanUnsafe(Attr key) {
			return null;
		}

	}

}
