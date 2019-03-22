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

package org.springframework.cloud.client.hypermedia;

import org.springframework.hateoas.client.Traverson;

/**
 * Callback to define the traversal to a resource.
 *
 * @author Oliver Gierke
 */
public interface TraversalDefinition {

	/**
	 * @param traverson The Traverson instance to run the traversal on.
	 * @return the builder for traversing
	 */
	Traverson.TraversalBuilder buildTraversal(Traverson traverson);

}
