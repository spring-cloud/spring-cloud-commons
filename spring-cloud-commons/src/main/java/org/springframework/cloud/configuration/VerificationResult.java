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

package org.springframework.cloud.configuration;

import java.util.Objects;

import org.springframework.core.style.ToStringCreator;
import org.springframework.util.StringUtils;

/**
 * @author Marcin Grzejszczak
 * @author Olga Maciaszek-Sharma
 */
final class VerificationResult {

	final String description;

	final String action;

	// if OK
	private VerificationResult() {
		this.description = "";
		this.action = "";
	}

	// if not OK
	private VerificationResult(String errorDescription, String action) {
		this.description = errorDescription;
		this.action = action;
	}

	static VerificationResult compatible() {
		return new VerificationResult();
	}

	static VerificationResult notCompatible(String errorDescription, String action) {
		return new VerificationResult(errorDescription, action);
	}

	boolean isNotCompatible() {
		return StringUtils.hasText(this.description) || StringUtils.hasText(this.action);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof VerificationResult)) {
			return false;
		}
		VerificationResult that = (VerificationResult) o;
		return description.equals(that.description) && action.equals(that.action);
	}

	@Override
	public int hashCode() {
		return Objects.hash(description, action);
	}

	@Override
	public String toString() {
		ToStringCreator toStringCreator = new ToStringCreator(this);
		toStringCreator.append("description", description);
		toStringCreator.append("action", action);
		return toStringCreator.toString();
	}

}
