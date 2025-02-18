/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.cloud.context.refresh;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.Lifecycle;

/**
 * A {@link Lifecycle} implementation that triggers {@link ContextRefresher#refresh()} to
 * be called on restart.
 *
 * @author Olga Maciaszek-Sharma
 * @since 4.1.0
 */
public class RefreshScopeLifecycle implements Lifecycle {

	private static final Log LOG = LogFactory.getLog(RefreshScopeLifecycle.class);

	private final ContextRefresher contextRefresher;

	private final Object lifecycleMonitor = new Object();

	private volatile boolean running = true;

	public RefreshScopeLifecycle(ContextRefresher contextRefresher) {
		this.contextRefresher = contextRefresher;
	}

	@Override
	public void start() {
		synchronized (lifecycleMonitor) {
			if (!isRunning()) {
				if (LOG.isInfoEnabled()) {
					LOG.info("Refreshing context on restart.");
				}
				Set<String> keys = contextRefresher.refresh();
				if (LOG.isInfoEnabled()) {
					LOG.info("Refreshed keys: " + keys);
				}
				running = true;
			}
		}
	}

	@Override
	public void stop() {
		synchronized (lifecycleMonitor) {
			if (isRunning()) {
				running = false;
			}
		}
	}

	@Override
	public boolean isRunning() {
		return running;
	}

}
