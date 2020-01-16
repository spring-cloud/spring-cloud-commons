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

/**
 * Contains information relevant to the request.
 *
 * @author Olga Maciaszek-Sharma
 */
public class DefaultRequestContext {

	/**
	 * A {@link String} value of hint that can be used to choose the correct service
	 * instance.
	 */
	private String hint = "default";

	public DefaultRequestContext() {
	}

	public DefaultRequestContext(String hint) {
		this.hint = hint;
	}

	public String getHint() {
		return hint;
	}

	public void setHint(String hint) {
		this.hint = hint;
	}

}
