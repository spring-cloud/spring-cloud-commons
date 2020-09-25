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

package org.springframework.cloud.bootstrap;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.util.ClassUtils;

/**
 * Cleans up the logging system immediately after the bootstrap context is created on
 * startup. Logging will go dark until the ConfigFileApplicationListener fires, but this
 * is the price we pay for that listener being able to adjust the log levels according to
 * what it finds in its own configuration.
 *
 * @author Dave Syer
 */
public class LoggingSystemShutdownListener
		implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

	/**
	 * Default order for the listener.
	 */
	public static final int DEFAULT_ORDER = BootstrapApplicationListener.DEFAULT_ORDER + 1;

	private int order = DEFAULT_ORDER;

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		shutdownLogging();
	}

	private void shutdownLogging() {
		// TODO: only enable if bootstrap and legacy
		LoggingSystem loggingSystem = LoggingSystem.get(ClassUtils.getDefaultClassLoader());
		loggingSystem.cleanUp();
		loggingSystem.beforeInitialize();
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

}
