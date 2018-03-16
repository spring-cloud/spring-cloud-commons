/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.client.discovery;

import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.cloud.commons.util.SpringFactoryImportSelector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.type.AnnotationMetadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author Spencer Gibb
 * @author Ryan Baxter
 */
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class EnableDiscoveryClientImportSelector
		extends SpringFactoryImportSelector<EnableDiscoveryClient> {

	@Override
	public String[] selectImports(AnnotationMetadata metadata) {
		String[] imports = super.selectImports(metadata);

		AnnotationAttributes attributes = AnnotationAttributes.fromMap(
				metadata.getAnnotationAttributes(getAnnotationClass().getName(), true));

		boolean autoRegister = attributes.getBoolean("autoRegister");

		if (autoRegister) {
			List<String> importsList = new ArrayList<>(Arrays.asList(imports));
			importsList.add("org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationConfiguration");
			imports = importsList.toArray(new String[0]);
		} else {
			disableAutoRegistration();

		}
		return imports;
	}

	private void disableAutoRegistration() {
		Environment env = getEnvironment();
		if(!env.containsProperty("spring.cloud.service-registry.auto-registration.enabled")) {
			if (ConfigurableEnvironment.class.isInstance(env)) {
				ConfigurableEnvironment configEnv = (ConfigurableEnvironment) env;
				LinkedHashMap<String, Object> map = new LinkedHashMap<>();
				map.put("spring.cloud.service-registry.auto-registration.enabled", false);
				MapPropertySource propertySource = new MapPropertySource(
						"springCloudDiscoveryClient", map);
				configEnv.getPropertySources().addLast(propertySource);
			}
		}
	}

	@Override
	protected boolean isEnabled() {
		boolean enabled =  new RelaxedPropertyResolver(getEnvironment()).getProperty(
				"spring.cloud.discovery.enabled", Boolean.class, Boolean.TRUE);
		if(!enabled) {
			disableAutoRegistration();
		}
		return enabled;
	}

	@Override
	protected boolean hasDefaultFactory() {
		return true;
	}

}
