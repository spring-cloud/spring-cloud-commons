/*
 * Copyright 2019-2019 the original author or authors.
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

package reactor.core.publisher;

import org.reactivestreams.Publisher;

/**
 * @author Tim Ysewyn
 */
public abstract class CloudFlux<T> extends Flux<T> {

	/**
	 * Pick the first {@link Publisher} to emit an onNext signal and replay all signals
	 * from that {@link Publisher}, effectively behaving like the fastest of these
	 * competing sources.
	 * @param sources The competing source publishers
	 * @param <I> The type of values in both source and output sequences
	 * @return a new {@link Flux} behaving like the fastest of its sources
	 */
	@SafeVarargs
	public static <I> Flux<I> firstNonEmpty(Publisher<? extends I>... sources) {
		return onAssembly(new FluxFirstNonEmptyEmitting<>(sources));
	}

	/**
	 * Pick the first {@link Publisher} to emit an onNext signal and replay all signals
	 * from that {@link Publisher}, effectively behaving like the fastest of these
	 * competing sources.
	 * @param sources The competing source publishers
	 * @param <I> The type of values in both source and output sequences
	 * @return a new {@link reactor.core.publisher.Flux} behaving like the fastest of its
	 * sources
	 */
	public static <I> Flux<I> firstNonEmpty(
			Iterable<? extends Publisher<? extends I>> sources) {
		return onAssembly(new FluxFirstNonEmptyEmitting<>(sources));
	}

}
