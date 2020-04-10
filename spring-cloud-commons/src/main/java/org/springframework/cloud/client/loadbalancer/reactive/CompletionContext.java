/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.cloud.client.loadbalancer.reactive;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.style.ToStringCreator;

/**
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 */
public class CompletionContext {

	private final Status status;

	private final Throwable throwable;

	private final Map<Object, Object> metrics;

	public CompletionContext(Status status) {
		this(status, null, new HashMap<>());
	}

	public CompletionContext(Status status, Throwable throwable) {
		this(status, throwable, new HashMap<>());
	}

	public CompletionContext(Status status, Map<Object, Object> metrics) {
		this(status, null, metrics);
	}

	public CompletionContext(Status status, Throwable throwable, Map<Object, Object> metrics) {
		this.status = status;
		this.throwable = throwable;
		this.metrics = metrics;
	}

	public static CompletionContext success() {
		return new CompletionContext(Status.SUCCESS);
	}

	public static CompletionContext discard() {
		return new CompletionContext(Status.DISCARD);
	}

	public static CompletionContext failed(Throwable t) {
		return new CompletionContext(Status.FAILED, t);
	}

	public Status getStatus() {
		return this.status;
	}

	public Throwable getThrowable() {
		return this.throwable;
	}

	@Override
	public String toString() {
		ToStringCreator to = new ToStringCreator(this);
		to.append("status", this.status);
		to.append("throwable", this.throwable);
		to.append("metrics", mapToString(metrics));
		return to.toString();
	}

	private String mapToString(Map<Object, Object> map) {
		return map.keySet().stream().map(key -> key + "=" + map.get(String.valueOf(key)))
				.collect(Collectors.joining(", ", "{", "}"));
	}

	/**
	 * Request status state.
	 */
	public enum Status {

		/** Request was handled successfully. */
		SUCCESS,
		/** Request reached the server but failed due to timeout or internal error. */
		FAILED,
		/** Request did not go off box and should not be counted for statistics. */
		DISCARD,

	}

}
