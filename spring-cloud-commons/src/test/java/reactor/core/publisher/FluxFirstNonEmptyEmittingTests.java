package reactor.core.publisher;

import java.time.Duration;
import java.util.Arrays;

import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.test.StepVerifier;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tim Ysewyn
 */
public class FluxFirstNonEmptyEmittingTests {

	@Test(expected = NullPointerException.class)
	public void arrayNull() {
		CloudFlux.firstNonEmpty((Publisher<Integer>[]) null);
	}

	@Test(expected = NullPointerException.class)
	public void iterableNull() {
		new FluxFirstEmitting<>((Iterable<Publisher<Integer>>) null);
	}

	@Test
	public void firstWinner() {
		StepVerifier.create(CloudFlux.firstNonEmpty(Flux.range(1, 10), Flux.range(11, 10)))
		.expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).verifyComplete();
	}

	@Test
	public void firstWinnerBackpressured() {
		StepVerifier.create(CloudFlux.firstNonEmpty(Flux.range(1, 10), Flux.range(11, 10)))
				.thenRequest(5)
				.expectNext(1, 2, 3, 4, 5)
				.thenCancel()
				.verifyThenAssertThat()
				.hasNotDiscardedElements()
				.hasNotDroppedElements()
				.hasNotDroppedErrors();
	}

	@Test
	public void secondWinner() {
		StepVerifier.create(CloudFlux.firstNonEmpty(Flux.never(),
				Flux.range(11, 10)
						.log()))
		.expectNext(11, 12, 13, 14, 15, 16, 17, 18, 19, 20).verifyComplete();
	}

	@Test
	public void secondEmitsError() {
		RuntimeException ex = new RuntimeException("forced failure");
		StepVerifier.create(CloudFlux.firstNonEmpty(Flux.never(), Flux.<Integer>error(ex)))
				.thenCancel()
				.verifyThenAssertThat()
				.hasNotDiscardedElements()
				.hasNotDroppedElements()
				.hasNotDroppedErrors();
	}

	@Test
	public void singleArrayNullSource() {
		StepVerifier.create(CloudFlux.firstNonEmpty((Publisher<Object>) null))
				.expectError(NullPointerException.class).verify();
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
		StepVerifier.create(CloudFlux.firstNonEmpty(Arrays.asList(Flux.never(),
				(Publisher<Object>) null,
				Flux.never())))
				.expectError(NullPointerException.class).verify();
	}

	@Test
	public void scanSubscriber() {
		CoreSubscriber<String> actual = new LambdaSubscriber<>(null, e -> {}, null, null);
		FluxFirstEmitting.RaceCoordinator<String> parent = new FluxFirstEmitting.RaceCoordinator<>(1);
		FluxFirstEmitting.FirstEmittingSubscriber<String> test = new FluxFirstEmitting.FirstEmittingSubscriber<>(actual, parent, 1);
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
		CoreSubscriber<String> actual = new LambdaSubscriber<>(null, e -> {}, null, null);
		FluxFirstEmitting.RaceCoordinator<String> parent = new FluxFirstEmitting.RaceCoordinator<>(1);
		FluxFirstEmitting.FirstEmittingSubscriber<String> test = new FluxFirstEmitting.FirstEmittingSubscriber<>(actual, parent, 1);
		Subscription sub = Operators.emptySubscription();
		test.onSubscribe(sub);

		assertThat(test.scan(Scannable.Attr.PARENT)).isSameAs(sub);
		assertThat(test.scan(Scannable.Attr.ACTUAL)).isSameAs(actual);
		assertThat(parent.scan(Scannable.Attr.CANCELLED)).isFalse();
		parent.cancelled = true;
		assertThat(parent.scan(Scannable.Attr.CANCELLED)).isTrue();
	}

}
