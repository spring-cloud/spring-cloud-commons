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

package org.springframework.cloud.client.loadbalancer;

import java.util.Objects;

import org.springframework.core.style.ToStringCreator;

/**
 * Allows propagating hints to the LoadBalancer.
 *
 * @author Olga Maciaszek-Sharma
 */
public class HintRequestContext implements TimedRequestContext {

	/**
	 * A {@link String} value of hint that can be used to choose the correct service
	 * instance.
	 */
	private String hint = "default";

	private long requestStartTime;

	public HintRequestContext() {
	}

	public HintRequestContext(String hint) {
		this.hint = hint;
	}

	public String getHint() {
		return hint;
	}

	public void setHint(String hint) {
		this.hint = hint;
	}

	@Override
	public long getRequestStartTime() {
		return requestStartTime;
	}

	@Override
	public void setRequestStartTime(long requestStartTime) {
		this.requestStartTime = requestStartTime;
	}

	@Override
	public String toString() {
		ToStringCreator to = new ToStringCreator(this);
		to.append("hint", hint);
		return to.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof HintRequestContext)) {
			return false;
		}
		HintRequestContext that = (HintRequestContext) o;
		return Objects.equals(hint, that.hint);
	}

	@Override
	public int hashCode() {
		return Objects.hash(hint);
	}

}
