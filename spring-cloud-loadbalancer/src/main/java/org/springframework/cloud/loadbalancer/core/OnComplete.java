/*
 * Copyright 2013-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.loadbalancer.core;

import org.springframework.core.style.ToStringCreator;

/**
 * @author Spencer Gibb
 */
//TODO: add metrics
public class OnComplete {

	public enum Status {
		/** Request was handled successfully */
		SUCCESSS,
		/** Request reached the server but failed due to timeout or internal error */
		FAILED,
		/** Request did not go off box and should not be counted for statistics */
		DISCARD,
	}

	private final Status status;
	private final Throwable throwable;

	public OnComplete(Status status) {
		this(status, null);
	}

	public OnComplete(Status status, Throwable throwable) {
		this.status = status;
		this.throwable = throwable;
	}

	public Status getStatus() {
		return status;
	}

	public Throwable getThrowable() {
		return throwable;
	}

	@Override
	public String toString() {
		ToStringCreator to = new ToStringCreator(this);
		to.append("status", status);
		to.append("throwable", throwable);
		return to.toString();
	}

}
