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

/**
 * @deprecated in favour of
 * {@link org.springframework.cloud.client.loadbalancer.CompletionContext}
 * @author Spencer Gibb
 */
// TODO: add metrics
@Deprecated
public class CompletionContext
		extends org.springframework.cloud.client.loadbalancer.CompletionContext {

	private final Status status;

	public CompletionContext(Status status) {
		this(status, null);
	}

	public CompletionContext(Status status, Throwable throwable) {
		super(resolveStatus(status), throwable);
		this.status = status;
	}

	public Status getStatus() {
		return this.status;
	}

	private static org.springframework.cloud.client.loadbalancer.CompletionContext.Status resolveStatus(
			Status status) {
		if (Status.SUCCESSS.equals(status)) {
			return org.springframework.cloud.client.loadbalancer.CompletionContext.Status.SUCCESS;
		}
		return org.springframework.cloud.client.loadbalancer.CompletionContext.Status
				.valueOf(status.name());
	}

	/**
	 * Request status state.
	 * @deprecated in favour of
	 * {@link org.springframework.cloud.client.loadbalancer.CompletionContext.Status}
	 */
	@Deprecated
	public enum Status {

		/** Request was handled successfully. */
		SUCCESSS,
		/** Request reached the server but failed due to timeout or internal error. */
		FAILED,
		/** Request did not go off box and should not be counted for statistics. */
		DISCARD,

	}

}
