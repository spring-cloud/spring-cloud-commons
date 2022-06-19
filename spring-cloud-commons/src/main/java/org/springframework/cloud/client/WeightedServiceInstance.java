/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.cloud.client;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a weighted instance of a service in a discovery system.
 *
 * @author Zhuozhi Ji
 */
public interface WeightedServiceInstance extends ServiceInstance {

	/**
	 * @return The raw weight of the service instance.
	 */
	default int getWeight() {
		return 0;
	}

	/**
	 * @return The current weight of the service instance.
	 */
	default AtomicInteger getCurrentWeight() {
		return new AtomicInteger(0);
	}

}
