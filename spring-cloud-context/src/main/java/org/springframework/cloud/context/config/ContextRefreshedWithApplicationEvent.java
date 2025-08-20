/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.cloud.context.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * A custom event for spring cloud context use cases that need the saved Spring
 * Application. This prevents a duplicated ApplicationPreparedEvent from being
 * republished.
 */
public class ContextRefreshedWithApplicationEvent extends SpringApplicationEvent {

	private final ConfigurableApplicationContext context;

	/**
	 * Create a new {@link ContextRefreshedWithApplicationEvent} instance.
	 * @param application the current application
	 * @param args the arguments the application is running with
	 * @param context the ApplicationContext about to be refreshed
	 */
	public ContextRefreshedWithApplicationEvent(SpringApplication application, String[] args,
			ConfigurableApplicationContext context) {
		super(application, args);
		this.context = context;
	}

	/**
	 * Return the application context.
	 * @return the context
	 */
	public ConfigurableApplicationContext getApplicationContext() {
		return this.context;
	}

}
