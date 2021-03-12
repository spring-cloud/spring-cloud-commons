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

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

/**
 * INTERNAL USAGE ONLY. This functionality will be ported to reactor-core and will be
 * removed in a future release.
 *
 * @author Tim Ysewyn
 */
public abstract class CloudFlux<T> extends Flux<T> {

	/**
	 * Pick the first {@link Publisher} to emit an onNext/onError signal and replay all
	 * signals from that {@link Publisher}, effectively behaving like the fastest of these
	 * competing sources. If all the sources complete empty, a single completion signal is
	 * sent. Note that if all the sources are empty (never emit an element, ie. no onNext)
	 * AND at least one is also infinite (no onComplete/onError signal), the resulting
	 * {@link Flux} will be infinite and empty (like {@link Flux#never()}).
	 * @param sources The competing source publishers
	 * @param <I> The type of values in both source and output sequences
	 * @return a new {@link Flux} behaving like the fastest of its sources
	 */
	@SafeVarargs
	public static <I> Flux<I> firstNonEmpty(Publisher<? extends I>... sources) {
		return onAssembly(new FluxFirstNonEmptyEmitting<>(sources));
	}

	/**
	 * Pick the first {@link Publisher} to emit an onNext/onError signal and replay all
	 * signals from that {@link Publisher}, effectively behaving like the fastest of these
	 * competing sources. If all the sources complete empty, a single completion signal is
	 * sent. Note that if all the sources are empty (never emit an element, ie. no onNext)
	 * AND at least one is also infinite (no onComplete/onError signal), the resulting
	 * {@link Flux} will be infinite and empty (like {@link Flux#never()}).
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
