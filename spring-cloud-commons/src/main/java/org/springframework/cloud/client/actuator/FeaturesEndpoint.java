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

package org.springframework.cloud.client.actuator;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author Spencer Gibb
 */
@Endpoint(id = "features")
public class FeaturesEndpoint implements ApplicationContextAware {

	private final List<HasFeatures> hasFeaturesList;

	private ApplicationContext context;

	public FeaturesEndpoint(List<HasFeatures> hasFeaturesList) {
		this.hasFeaturesList = hasFeaturesList;
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}

	@ReadOperation
	public Features features() {
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
		features.getEnabled().add(new Feature(feature.getName(), type.getCanonicalName(),
				type.getPackage().getImplementationVersion(), type.getPackage().getImplementationVendor()));
	}

	static class Features {

		final List<Feature> enabled = new ArrayList<>();

		final List<String> disabled = new ArrayList<>();

		public List<Feature> getEnabled() {
			return this.enabled;
		}

		public List<String> getDisabled() {
			return this.disabled;
		}

	}

	static class Feature {

		final String type;

		final String name;

		final String version;

		final String vendor;

		Feature(String name, String type, String version, String vendor) {
			this.type = type;
			this.name = name;
			this.version = version;
			this.vendor = vendor;
		}

		public String getType() {
			return this.type;
		}

		public String getName() {
			return this.name;
		}

		public String getVersion() {
			return this.version;
		}

		public String getVendor() {
			return this.vendor;
		}

		@Override
		public String toString() {
			return "Feature{" + "type='" + this.type + '\'' + ", name='" + this.name + '\'' + ", version='"
					+ this.version + '\'' + ", vendor='" + this.vendor + '\'' + '}';
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			Feature feature = (Feature) o;

			if (this.type != null ? !this.type.equals(feature.type) : feature.type != null) {
				return false;
			}
			if (this.name != null ? !this.name.equals(feature.name) : feature.name != null) {
				return false;
			}
			if (this.version != null ? !this.version.equals(feature.version) : feature.version != null) {
				return false;
			}
			return this.vendor != null ? this.vendor.equals(feature.vendor) : feature.vendor == null;
		}

		@Override
		public int hashCode() {
			int result = this.type != null ? this.type.hashCode() : 0;
			result = 31 * result + (this.name != null ? this.name.hashCode() : 0);
			result = 31 * result + (this.version != null ? this.version.hashCode() : 0);
			result = 31 * result + (this.vendor != null ? this.vendor.hashCode() : 0);
			return result;
		}

	}

}
