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

package org.springframework.cloud.context.environment;

import java.util.Collections;
import java.util.Map;

import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.EndpointWebExtension;
import org.springframework.boot.actuate.env.EnvironmentEndpointWebExtension;

/**
 * MVC endpoint for the {@link EnvironmentManager}, providing a POST to /env as a simple
 * way to change the Environment.
 *
 * @author Dave Syer
 *
 */
@EndpointWebExtension(endpoint = WritableEnvironmentEndpoint.class)
public class WritableEnvironmentEndpointWebExtension
		extends EnvironmentEndpointWebExtension {

	private EnvironmentManager environment;

	public WritableEnvironmentEndpointWebExtension(WritableEnvironmentEndpoint endpoint,
			EnvironmentManager environment) {
		super(endpoint);
		this.environment = environment;
	}

	@WriteOperation
	public Object write(String name, String value) {
		this.environment.setProperty(name, value);
		return Collections.singletonMap(name, value);
	}

	@DeleteOperation
	public Map<String, Object> reset() {
		return this.environment.reset();
	}

	public void setEnvironmentManager(EnvironmentManager environment) {
		this.environment = environment;
	}

}
