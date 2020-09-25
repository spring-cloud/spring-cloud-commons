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

package org.springframework.cloud.context.refresh;

import org.springframework.boot.DefaultBootstrapContext;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;

/**
 * @author Dave Syer
 * @author Venil Noronha
 */
public class ConfigDataContextRefresher extends ContextRefresher {

	public ConfigDataContextRefresher(ConfigurableApplicationContext context, RefreshScope scope) {
		super(context, scope);
	}

	@Override
	protected void updateEnvironment() {
		if (logger.isTraceEnabled()) {
			logger.trace("Re-processing environment to add config data");
		}
		StandardEnvironment environment = copyEnvironment(getContext().getEnvironment());
		String[] activeProfiles = getContext().getEnvironment().getActiveProfiles();
		DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
		ConfigDataEnvironmentPostProcessor.applyTo(environment, resourceLoader, new DefaultBootstrapContext(),
				activeProfiles);

		if (environment.getPropertySources().contains(REFRESH_ARGS_PROPERTY_SOURCE)) {
			environment.getPropertySources().remove(REFRESH_ARGS_PROPERTY_SOURCE);
		}
		MutablePropertySources target = getContext().getEnvironment().getPropertySources();
		String targetName = null;
		for (PropertySource<?> source : environment.getPropertySources()) {
			String name = source.getName();
			if (target.contains(name)) {
				targetName = name;
			}
			if (!this.standardSources.contains(name)) {
				if (target.contains(name)) {
					target.replace(name, source);
				}
				else {
					if (targetName != null) {
						target.addAfter(targetName, source);
						// update targetName to preserve ordering
						targetName = name;
					}
					else {
						// targetName was null so we are at the start of the list
						target.addFirst(source);
						targetName = name;
					}
				}
			}
		}
	}

}
