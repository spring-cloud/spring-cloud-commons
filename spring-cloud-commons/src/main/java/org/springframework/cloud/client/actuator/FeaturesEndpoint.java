/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.cloud.client.actuator;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;


/**
 * @author Spencer Gibb
 */
@ConfigurationProperties(prefix = "endpoints.features", ignoreUnknownFields = false)
public class FeaturesEndpoint extends AbstractEndpoint<FeaturesEndpoint.Features>
		implements ApplicationContextAware {

	private final List<HasFeatures> hasFeaturesList;
	private ApplicationContext context;

	public FeaturesEndpoint(List<HasFeatures> hasFeaturesList) {
		super("features", false);
		this.hasFeaturesList = hasFeaturesList;
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}

	@Override
	public Features invoke() {
		Features features = new Features();

		for (HasFeatures hasFeatures : this.hasFeaturesList) {
			List<Class<?>> abstractFeatures = hasFeatures.getAbstractFeatures();
			if (abstractFeatures != null) {
				for (Class<?> clazz : abstractFeatures) {
					addAbstractFeature(features, clazz);
				}
			}

			List<NamedFeature> namedFeatures = hasFeatures.getNamedFeatures();
			if (namedFeatures != null) {
				for (NamedFeature namedFeature : namedFeatures) {
					addFeature(features, namedFeature);
				}
			}
		}

		return features;
	}

	private void addAbstractFeature(Features features, Class<?> type) {
		String featureName = type.getSimpleName();
		try {
			Object bean = this.context.getBean(type);
			Class<?> beanClass = bean.getClass();
			addFeature(features, new NamedFeature(featureName, beanClass));
		}
		catch (NoSuchBeanDefinitionException e) {
			features.getDisabled().add(featureName);
		}
	}

	private void addFeature(Features features, NamedFeature feature) {
		Class<?> type = feature.getType();
		features.getEnabled()
				.add(new Feature(feature.getName(), type.getCanonicalName(),
						type.getPackage().getImplementationVersion(),
						type.getPackage().getImplementationVendor()));
	}

	class Features {
		final List<Feature> enabled = new ArrayList<>();
		final List<String> disabled = new ArrayList<>();

		public List<Feature> getEnabled() {
			return enabled;
		}

		public List<String> getDisabled() {
			return disabled;
		}
	}

	
	class Feature {
		final String type;
		final String name;
		final String version;
		final String vendor;

		public Feature(String type, String name, String version, String vendor) {
			this.type = type;
			this.name = name;
			this.version = version;
			this.vendor = vendor;
		}

		public String getType() {
			return type;
		}

		public String getName() {
			return name;
		}

		public String getVersion() {
			return version;
		}

		public String getVendor() {
			return vendor;
		}
	}
}
