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

package org.springframework.cloud.client.loadbalancer.reactive;

import org.springframework.core.style.ToStringCreator;
import org.springframework.web.reactive.function.client.ClientResponse;

/**
 * @author Spencer Gibb
 */
// TODO: add metrics
public class CompletionContext {

	private final Status status;

	private final Throwable throwable;

	private final ClientResponse clientResponse;

	public CompletionContext(Status status) {
		this(status, null, null);
	}

	public CompletionContext(Status status, ClientResponse clientResponse) {
		this(status, clientResponse, null);
	}

	public CompletionContext(Status status, Throwable throwable) {
		this(status, null, throwable);
	}

	public CompletionContext(Status status, ClientResponse clientResponse, Throwable throwable) {
		this.status = status;
		this.clientResponse = clientResponse;
		this.throwable = throwable;
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
		return to.toString();
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
