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

package org.springframework.cloud.client.hypermedia;

import java.util.List;

import org.springframework.scheduling.config.ContextLifecycleScheduledTaskRegistrar;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * A {@link ScheduledTaskRegistrar} that verifies all {@link DiscoveredResource} instances
 * in the system, based on the given timing configuration.
 *
 * @author Oliver Gierke
 */
public class RemoteResourceRefresher extends ContextLifecycleScheduledTaskRegistrar {

	private final List<RemoteResource> discoveredResources;

	private final int fixedDelay;

	private final int initialDelay;

	public RemoteResourceRefresher(List<RemoteResource> discoveredResources, int fixedDelay, int initialDelay) {
		this.discoveredResources = discoveredResources;
		this.fixedDelay = fixedDelay;
		this.initialDelay = initialDelay;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.scheduling.config.ContextLifecycleScheduledTaskRegistrar#
	 * afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() {

		for (final RemoteResource resource : this.discoveredResources) {
			addFixedDelayTask(new IntervalTask(new Runnable() {

				@Override
				public void run() {
					resource.verifyOrDiscover();
				}
			}, this.fixedDelay, this.initialDelay));
		}

		super.afterPropertiesSet();
	}

}
