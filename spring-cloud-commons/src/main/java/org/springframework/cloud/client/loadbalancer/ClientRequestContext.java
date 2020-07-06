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

import org.springframework.core.style.ToStringCreator;
import org.springframework.web.reactive.function.client.ClientRequest;

/**
 * @author Olga Maciaszek-Sharma
 */
public class ClientRequestContext {

	private final ClientRequest clientRequest;

	private String hint;

	public ClientRequestContext(ClientRequest clientRequest) {
		this(clientRequest, "default");
	}

	public ClientRequestContext(ClientRequest clientRequest, String hint) {
		this.clientRequest = clientRequest;
		this.hint = hint;
	}

	public ClientRequest getClientRequest() {
		return clientRequest;
	}

	public String getHint() {
		return hint;
	}

	public void setHint(String hint) {
		this.hint = hint;
	}

	@Override
	public String toString() {
		ToStringCreator to = new ToStringCreator(this);
		to.append("clientRequest", clientRequest);
		to.append("hint", hint);
		return to.toString();
	}

}
