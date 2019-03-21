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

package org.springframework.cloud.context.environment;

import org.springframework.boot.actuate.env.EnvironmentEndpoint;
import org.springframework.core.env.Environment;

/**
 * An extension of the standard {@link EnvironmentEndpoint} that allows to modify the
 * environment at runtime.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class WritableEnvironmentEndpoint extends EnvironmentEndpoint {

	public WritableEnvironmentEndpoint(Environment environment) {
		super(environment);
	}

}
