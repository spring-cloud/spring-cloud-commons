/*
 * Copyright 2013-2021 the original author or authors.
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

package org.springframework.cloud.context.test;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class TestEnvPostProcessor implements EnvironmentPostProcessor, Ordered {

	public static final String EPP_ENABLED = "configdatarefresh.epp.enabled";

	public static final String EPP_VALUE = "configdatarefresh.epp.count";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (environment.getProperty(EPP_ENABLED, Boolean.class, false)) {
			Map<String, Object> source = new HashMap<>();
			source.put("spring.cloud.refresh.additional-property-sources-to-retain", getClass().getSimpleName());
			source.put("spring.config.import", "testdatasource:");
			MapPropertySource propertySource = new MapPropertySource(getClass().getSimpleName(), source);
			environment.getPropertySources().addFirst(propertySource);
		}
	}

	@Override
	public int getOrder() {
		return ConfigDataEnvironmentPostProcessor.ORDER - 1;
	}

}
