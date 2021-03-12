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

package org.springframework.cloud.util.random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.RandomValuePropertySource;
import org.springframework.boot.env.RandomValuePropertySourceEnvironmentPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

/**
 * @author Ryan Baxter
 */
@Configuration(proxyBeanMethods = false)
public class CachedRandomPropertySourceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	private static final Log logger = LogFactory.getLog(CachedRandomPropertySourceEnvironmentPostProcessor.class);

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		MutablePropertySources propertySources = environment.getPropertySources();
		PropertySource<?> propertySource = propertySources.get(RandomValuePropertySource.RANDOM_PROPERTY_SOURCE_NAME);
		if (propertySource != null) {
			PropertySource<?> existing = propertySources.get(CachedRandomPropertySource.NAME);
			if (existing != null) {
				logger.trace("CachedRandomPropertySource already present");
				return;
			}
			propertySources.addLast(new CachedRandomPropertySource(propertySource));
			logger.trace("CachedRandomPropertySource added to Environment");
		}
	}

	@Override
	public int getOrder() {
		return RandomValuePropertySourceEnvironmentPostProcessor.ORDER + 1;
	}

}
