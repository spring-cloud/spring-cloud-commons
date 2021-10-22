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

package org.springframework.cloud.commons.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * @author Spencer Gibb
 * @since 3.1.0
 */
@Configuration(proxyBeanMethods = false)
public class CommonsConfigAutoConfiguration {

	@Bean
	public DefaultsBindHandlerAdvisor defaultsBindHandlerAdvisor(
			@Nullable DefaultsBindHandlerAdvisor.MappingsProvider[] providers) {
		Map<ConfigurationPropertyName, ConfigurationPropertyName> additionalMappings = new HashMap<>();
		if (!ObjectUtils.isEmpty(providers)) {
			for (int i = 0; i < providers.length; i++) {
				DefaultsBindHandlerAdvisor.MappingsProvider mappingsProvider = providers[i];
				additionalMappings.putAll(mappingsProvider.getDefaultMappings());
			}
		}
		return new DefaultsBindHandlerAdvisor(additionalMappings);
	}

}
